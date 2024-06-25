#include "react-native-simple-jsi.h"
#include <iostream>
#include <sstream>
#include <sys/statfs.h> // For statfs on Android
#include <fstream>
#include <vector>
#include <iterator>
#include <sys/sysinfo.h> // For RAM info
#include <unistd.h>		 // For system calls
#include <stdlib.h>		 // For system calls
#include <stdio.h>
#include <sys/stat.h>
#include <sys/wait.h>
#include <jsi/jsi.h>
#include <jsi/jsilib.h>
#include <thread>
#include <chrono>
#include <string>
#include <android/log.h>
#include <jni.h>
#include <android/asset_manager_jni.h>
#include <android/asset_manager.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <fcntl.h>

using namespace facebook::jsi;
// using namespace std;

namespace simplejsi
{
	static JavaVM *g_javaVM = nullptr;
    static JNIEnv *g_jniEnv = nullptr;

	static const char *TAG = "jsi-socket";

	double multiply(double a, double b)
	{
		return a * b;
	}
	double add(double a, double b)
	{
		return a + b;
	}

	// Function to register helloWorld in JSI
	void registerHelloWorld(Runtime &jsiRuntime)
	{
		auto helloWorld = Function::createFromHostFunction(
			jsiRuntime,
			PropNameID::forAscii(jsiRuntime, "helloWorld"),
			0,
			[](Runtime &runtime,
			   const Value &thisValue,
			   const Value *arguments,
			   size_t count) -> Value
			{
				std::string helloworld = "helloworld Ash 123";
				return Value(runtime, String::createFromUtf8(runtime, helloworld));
			});

		jsiRuntime.global().setProperty(jsiRuntime, "helloWorld", std::move(helloWorld));
	}

	void registerFoo(Runtime &jsiRuntime)
	{
		auto foo = Function::createFromHostFunction(
			jsiRuntime,
			PropNameID::forAscii(jsiRuntime, "foo"),
			1,
			[](Runtime &runtime, const Value &thisValue, const Value *arguments, size_t count) -> Value
			{
				if (count < 1 || !arguments[0].isObject() || !arguments[0].getObject(runtime).isFunction(runtime))
				{
					throw JSError(runtime, "Expected a function as the first argument");
				}

				// Create a shared reference to the user callback
				auto userCallbackRef = std::make_shared<Object>(arguments[0].getObject(runtime));

				// Lambda to be run in the new thread
				auto f = [&runtime, userCallbackRef]()
				{
					auto val = std::make_shared<std::string>("hello world");
					auto error = std::make_shared<Value>(Value::undefined());

					// Periodically log messages to Android log and sleep for 5 seconds
					while (true)
					{
						__android_log_print(ANDROID_LOG_INFO, "MyApp", "Log message to Android");
						userCallbackRef->asFunction(runtime).call(runtime, *error, *val);
						std::this_thread::sleep_for(std::chrono::seconds(5));
					}
				};

				// // Launch the lambda in a new thread
				std::thread thread_object(f);
				thread_object.join();

				return Value::undefined();
			});

		jsiRuntime.global().setProperty(jsiRuntime, "foo", std::move(foo));
	}

	std::string runCPP(void)
	{
		// return installAPKCMD("/storage/emulated/0/Download/testapp.apk"); // getCPUUsage();
		return uninstallAPKCMD("com.aiuspaktyn.ramusage"); // getCPUUsage();
	}

	// Define the function to get system information
	std::string getCPUUsage()
	{
		std::ostringstream oss;

		// Get RAM info
		struct sysinfo memInfo;
		sysinfo(&memInfo);
		long long totalRam = memInfo.totalram * memInfo.mem_unit;
		long long freeRam = (memInfo.freeram + memInfo.bufferram + memInfo.sharedram) * memInfo.mem_unit;

		// Calculate percentages
		float totalRamMB = totalRam / (1024 * 1024);
		float freeRamMB = freeRam / (1024 * 1024);
		float usedRamMB = totalRamMB - freeRamMB;
		float totalRamPercent = (usedRamMB / totalRamMB) * 100;
		float freeRamPercent = 100 - totalRamPercent;

		// Output to string stream
		oss << "Total RAM: " << totalRamMB << " MB\n";
		oss << "Free RAM: " << freeRamMB << " MB\n";
		oss << "Used RAM: " << usedRamMB << " MB\n";
		oss << "Total RAM Percentage: " << totalRamPercent << "%\n";
		oss << "Free RAM Percentage: " << freeRamPercent << "%\n";

		// Get ROM info
		std::ifstream cpuinfo("/proc/cpuinfo");
		std::string cpu;
		while (std::getline(cpuinfo, cpu))
		{
			if (cpu.find("model name") != std::string::npos)
			{
				std::istringstream iss(cpu.substr(cpu.find(":") + 2));
				oss << "CPU: " << iss.str() << "\n";
				break;
			}
		}

		// Get CPU usage
		std::ifstream file("/proc/stat");
		std::string line;
		while (std::getline(file, line))
		{
			if (line.find("cpu ") == 0)
			{
				std::istringstream iss(line);
				std::vector<std::string> tokens{std::istream_iterator<std::string>{iss}, std::istream_iterator<std::string>{}};
				if (tokens.size() >= 8)
				{
					long user = std::stol(tokens[1]);
					long nice = std::stol(tokens[2]);
					long system = std::stol(tokens[3]);
					long idle = std::stol(tokens[4]);
					long iowait = std::stol(tokens[5]);
					long irq = std::stol(tokens[6]);
					long softirq = std::stol(tokens[7]);
					long total = user + nice + system + idle + iowait + irq + softirq;
					long cpuUsage = 100 * (total - idle) / total;

					oss << "CPU Usage: " << cpuUsage << "%\n";
					break;
				}
			}
		}

		// Get storage info (Android)
		struct statfs buf;
		if (statfs("/data", &buf) >= 0)
		{
			long long totalSize = buf.f_blocks * buf.f_bsize;
			long long freeSize = buf.f_bfree * buf.f_bsize;
			oss << "Total Storage: " << totalSize / (1024 * 1024) << " MB\n";
			oss << "Free Storage: " << freeSize / (1024 * 1024) << " MB\n";
		}
		else
		{
			oss << "Failed to get storage information.\n";
		}

		return oss.str();
	}

	std::string installAPKCMD(const std::string &apkPath, std::function<void(std::string)> callback)
	{
		std::ostringstream resultBuilder;

		// Check if the file exists
		struct stat buffer;
		if (stat(apkPath.c_str(), &buffer) != 0)
		{
			resultBuilder << "APK file does not exist at path: " << apkPath;
			return resultBuilder.str();
		}

		// Construct the commands to grant permissions and install the APK using 'su' command
		std::string chmodCommand = "su -c 'chmod 777 " + apkPath + "'";
		std::string installCommand = "su -c 'pm install " + apkPath + "'";

		try
		{
			// Grant necessary permissions to the APK file
			int chmodResult = system(chmodCommand.c_str());
			if (chmodResult != 0)
			{
				resultBuilder << "Failed to set permissions on APK file: " << apkPath
							  << ". Error code: " << chmodResult;
				return resultBuilder.str();
			}

			// Perform the silent installation
			// callback("start");
			std::this_thread::sleep_for(std::chrono::milliseconds(2000));
			int installResult = system(installCommand.c_str());
			if (installResult == 0)
			{
				resultBuilder << "APK installed successfully from path: " << apkPath;
			}
			else
			{
				resultBuilder << "Failed to install APK from path: " << apkPath
							  << ". Error code: " << installResult;
			}
		}
		catch (const std::exception &e)
		{
			resultBuilder << "Exception occurred while installing APK: " << e.what();
		}

		// callback("end");
		return resultBuilder.str();
	}

	std::string uninstallAPKCMD(const std::string &packageName)
	{
		std::ostringstream resultBuilder;

		// Construct the command to uninstall the APK using 'pm' command
		std::string uninstallCommand = "su -c 'pm uninstall " + packageName + "'";

		try
		{
			// Perform the uninstallation
			int uninstallResult = system(uninstallCommand.c_str());
			if (uninstallResult == 0)
			{
				resultBuilder << "APK uninstalled successfully for package: " << packageName;
			}
			else
			{
				resultBuilder << "Failed to uninstall APK for package: " << packageName
							  << ". Error code: " << uninstallResult;
			}
		}
		catch (const std::exception &e)
		{
			resultBuilder << "Exception occurred while uninstalling APK: " << e.what();
		}

		return resultBuilder.str();
	}

	// Helper function to get the Downloads directory path
	std::string getDownloadsDirectoryPath(JNIEnv *env)
	{
		jclass envClass = env->FindClass("android/os/Environment");
		jmethodID getExternalStoragePublicDirectory = env->GetStaticMethodID(envClass, "getExternalStoragePublicDirectory", "(Ljava/lang/String;)Ljava/io/File;");
		jfieldID downloadsFieldID = env->GetStaticFieldID(envClass, "DIRECTORY_DOWNLOADS", "Ljava/lang/String;");
		jstring downloadsString = (jstring)env->GetStaticObjectField(envClass, downloadsFieldID);

		jobject fileObj = env->CallStaticObjectMethod(envClass, getExternalStoragePublicDirectory, downloadsString);
		jclass fileClass = env->FindClass("java/io/File");
		jmethodID getAbsolutePath = env->GetMethodID(fileClass, "getAbsolutePath", "()Ljava/lang/String;");
		jstring pathString = (jstring)env->CallObjectMethod(fileObj, getAbsolutePath);

		const char *pathCStr = env->GetStringUTFChars(pathString, NULL);
		std::string downloadsPath(pathCStr);
		env->ReleaseStringUTFChars(pathString, pathCStr);

		return downloadsPath;
	}

	void startSocketServer()
	{
		auto serverFunction = []()
		{
			if (g_javaVM->AttachCurrentThread(&g_jniEnv, NULL) != JNI_OK)
			{
				__android_log_print(ANDROID_LOG_ERROR, TAG, "Failed to attach current thread to JVM");
				return;
			}

			std::string downloadsDir = getDownloadsDirectoryPath(g_jniEnv);
			std::string filePath = downloadsDir + "/received_file.txt";
			// std::string filePath = "/received_file.txt";

			int server_fd, new_socket;
			struct sockaddr_in address;
			int opt = 1;
			int addrlen = sizeof(address);
			char buffer[1024] = {0};

			// Creating socket file descriptor
			if ((server_fd = socket(AF_INET, SOCK_STREAM, 0)) == 0)
			{
				__android_log_print(ANDROID_LOG_ERROR, TAG, "Socket creation failed");
				return;
			}
			__android_log_print(ANDROID_LOG_INFO, TAG, "Socket created successfully");

			// Forcefully attaching socket to the port 8080
			if (setsockopt(server_fd, SOL_SOCKET, SO_REUSEADDR | SO_REUSEPORT, &opt, sizeof(opt)))
			{
				__android_log_print(ANDROID_LOG_ERROR, TAG, "setsockopt failed");
				return;
			}
			__android_log_print(ANDROID_LOG_INFO, TAG, "setsockopt applied successfully");

			address.sin_family = AF_INET;
			address.sin_addr.s_addr = INADDR_ANY;
			address.sin_port = htons(8080);

			// Binding socket to the port 8080
			if (bind(server_fd, (struct sockaddr *)&address, sizeof(address)) < 0)
			{
				__android_log_print(ANDROID_LOG_ERROR, TAG, "bind failed");
				return;
			}
			__android_log_print(ANDROID_LOG_INFO, TAG, "Socket bound successfully");

			if (listen(server_fd, 3) < 0)
			{
				__android_log_print(ANDROID_LOG_ERROR, TAG, "listen failed");
				return;
			}
			__android_log_print(ANDROID_LOG_INFO, TAG, "Listening for connections");

			if ((new_socket = accept(server_fd, (struct sockaddr *)&address, (socklen_t *)&addrlen)) < 0)
			{
				__android_log_print(ANDROID_LOG_ERROR, TAG, "accept failed");
				return;
			}
			__android_log_print(ANDROID_LOG_INFO, TAG, "Connection accepted");

			std::ofstream outfile(filePath, std::ios::binary);
			int valread;
			while ((valread = read(new_socket, buffer, 1024)) > 0)
			{
				outfile.write(buffer, valread);
			}
			outfile.close();
			__android_log_print(ANDROID_LOG_INFO, TAG, "File received and saved to %s", filePath.c_str());

			close(new_socket);
			close(server_fd);
			__android_log_print(ANDROID_LOG_INFO, TAG, "Socket closed");

			g_javaVM->DetachCurrentThread();
			__android_log_print(ANDROID_LOG_INFO, TAG, "Detached thread from JVM");
		};

		std::thread serverThread(serverFunction);
		serverThread.detach();
	}

	void registerStartSocketServer(Runtime &runtime)
	{
		auto startSocketServerFunc = Function::createFromHostFunction(
			runtime,
			PropNameID::forAscii(runtime, "startSocketServer"),
			0,
			[](Runtime &runtime, const Value &thisValue, const Value *arguments, size_t count) -> Value
			{
				startSocketServer();
				return Value::undefined();
			});

		runtime.global().setProperty(runtime, "startSocketServer", std::move(startSocketServerFunc));
	}

	void install(Runtime &jsiRuntime)
	{
		// Register the helloWorld function
		registerHelloWorld(jsiRuntime);
		registerFoo(jsiRuntime);
		registerStartSocketServer(jsiRuntime);
	}


	extern "C" jint JNI_OnLoad(JavaVM *vm, void *reserved)
	{
		g_javaVM = vm;
		return JNI_VERSION_1_6;
	}
}
