function fn() {
  var gatewayUrl =
    java.lang.System.getenv("GATEWAY_URL") || "http://localhost:28080";
  var org = java.lang.System.getenv("ORG") || "catsOrg";
  var service = java.lang.System.getenv("SERVICE") || "booking";

  var config = {
    gatewayUrl: gatewayUrl,
    org: org,
    service: service,
    basePath: "/cats/" + org + "/" + service,
  };

  karate.configure("connectTimeout", 10000);
  karate.configure("readTimeout", 30000);

  return config;
}
