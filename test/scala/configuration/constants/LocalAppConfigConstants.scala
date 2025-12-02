package configuration.constants

import configuration.models.*
import configuration.AppConfig

object LocalAppConfigConstants {

  val featureSwitches =
    FeatureSwitches(
      useDockerHost = false,
      localTesting = true,
      useCors = true
    )

  val devIrlFrontendConfig =
    DevIrlFrontendConfig(
      host = "0.0.0.0",
      port = 3000,
      baseUrl = "http://localhost:3000"
    )

  val appServerConfig =
    ServerConfig(
      host = "0.0.0.0",
      port = 8090
    )

  val containerPostgreSqlConfig =
    PostgresqlConfig(
      dbName = "dev_auth_db",
      dockerHost = "dev-auth-db",
      host = "localhost",
      port = 5433,
      username = "dev_auth_user",
      password = "turnip",
      maxPoolSize = 42
    )

  val redisConfig =
    RedisConfig(
      dockerHost = "redis-container",
      host = "localhost",
      port = 6379
    )

  val kafkaConfig =
    KafkaConfig(
      bootstrapServers = "localhost:9092",
      clientId = "devirl-auth-service",
      acks = "all",
      lingerMs = 5,
      retries = 10,
      topic = KafkaTopicConfig(
        "user.registered.v1"
      )
    )

  val localAppConfigConstant =
    AppConfig(
      featureSwitches = featureSwitches,
      devIrlFrontendConfig = devIrlFrontendConfig,
      redisConfig = redisConfig,
      kafkaConfig = kafkaConfig,
      postgresqlConfig = containerPostgreSqlConfig,
      serverConfig = appServerConfig
    )
}
