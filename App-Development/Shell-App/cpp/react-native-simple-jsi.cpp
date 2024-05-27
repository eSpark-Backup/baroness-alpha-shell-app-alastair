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
#include <thread>
#include <chrono>

using namespace facebook::jsi;

namespace simplejsi
{
	double multiply(double a, double b)
	{
		return a * b;
	}
	double add(double a, double b)
	{
		return a + b;
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

	void install(Runtime &jsiRuntime)
	{

		// auto helloWorld = Function::createFromHostFunction(jsiRuntime,
		// 												   PropNameID::forAscii(jsiRuntime,
		// 																		"helloWorld"),
		// 												   0,
		// 												   [](Runtime &runtime,
		// 													  const Value &thisValue,
		// 													  const Value *arguments,
		// 													  size_t count) -> Value
		// 												   {
		// 													   std::string helloworld = "helloworld";

		// 													   return Value(runtime, String::createFromUtf8(
		// 																								runtime,
		// 																								helloworld));
		// 												   });

		// jsiRuntime.global().setProperty(jsiRuntime, "helloWorld", std::move(helloWorld));
	}
}

// using namespace facebook;

// // Define a function to call a JS callback
// void invokeJSCallback(jsi::Runtime &rt, const jsi::Value &jsCallback, const std::string &message)
// {
//     if (jsCallback.isObject() && jsCallback.asObject(rt).isFunction(rt))
//     {
//         auto callback = jsCallback.asObject(rt).asFunction(rt);
//         jsi::String resultMessage = jsi::String::createFromUtf8(rt, message);
//         callback.call(rt, resultMessage);
//     }
//     else
//     {
//         throw jsi::JSError(rt, "Expected a function as the first argument");
//     }
// }

// // Define the function exposed to JavaScript
// jsi::Value invokeCallback(jsi::Runtime &rt, const jsi::Value &thisValue, const jsi::Value *arguments, size_t count)
// {
//     if (count != 2 || !arguments[0].isString() || !arguments[1].isObject() || !arguments[1].asObject(rt).isFunction(rt))
//     {
//         throw jsi::JSError(rt, "invokeCallback expects a string and a function as arguments");
//     }

//     std::string message = arguments[0].asString(rt).utf8(rt);
//     jsi::Value jsCallback = arguments[1];
//     invokeJSCallback(rt, jsCallback, message);
//     return jsi::Value::undefined();
// }

// // Install the function into the runtime
// void install(jsi::Runtime &rt)
// {
//     auto invokeCallbackWrapper = [](jsi::Runtime &rt, const jsi::Value &thisValue, const jsi::Value *arguments, size_t count) -> jsi::Value
//     {
//         return invokeCallback(rt, thisValue, arguments, count);
//     };

//     rt.global().setProperty(rt, "invokeCallback", jsi::Function::createFromHostFunction(rt, jsi::PropNameID::forAscii(rt, "invokeCallback"), 2, invokeCallbackWrapper));
// }