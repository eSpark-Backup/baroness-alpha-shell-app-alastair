#ifndef SIMPLEJSI_H
#define SIMPLEJSI_H

#include <iostream>
#include <string>

namespace facebook {
namespace jsi {
class Runtime;
}
}

namespace simplejsi {
  double multiply(double a, double b);
  double add(double a, double b);
  std::string runCPP(void);
  std::string getCPUUsage(void);
  std::string installAPKCMD(const std::string &apkPath, std::function<void(std::string)> callback);
  std::string uninstallAPKCMD(const std::string &packageName);
  void install(facebook::jsi::Runtime &jsiRuntime);
  void registerHelloWorld(facebook::jsi::Runtime &jsiRuntime);
  void registerStartSocketServer(facebook::jsi::Runtime &runtime);
  void registerFoo(facebook::jsi::Runtime &jsiRuntime);
}

#endif /* SIMPLEJSI_H */
