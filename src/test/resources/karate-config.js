function fn() {
  var env = karate.env; // get system property 'karate.env'
  karate.log('karate.env system property was:', env);
  if (!env) {
    env = 'dev';
  }
  var config = {
    env: env,
	 apiUrl: 'http://localhost:8080'
  }
  if (env == 'dev') {
    // customize
    // e.g. config.foo = 'bar';
  } else if (env == 'prod') {
    apiUrl: 'https://limitless-ridge-11429.herokuapp.com/'
  }
  return config;
}