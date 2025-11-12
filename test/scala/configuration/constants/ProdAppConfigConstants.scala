package configuration.constants

import configuration.models.*
import configuration.AppConfig

object ProdAppConfigConstants {

  val featureSwitches =
    FeatureSwitches(
      useDockerHost = false,
      localTesting = false,
      useCors = false
    )

  val devIrlFrontendConfig =
    DevIrlFrontendConfig(
      host = "0.0.0.0",
      port = 8080,
      baseUrl = "https://devirl.com"
    )

  val appServerConfig =
    ServerConfig(
      host = "0.0.0.0",
      port = 8080
    )

  val containerPostgreSqlConfig =
    PostgresqlConfig(
      dbName = "dev_notification_db",
      dockerHost = "dev-auth-db",
      host = "localhost",
      port = 5433,
      username = "dev_notification_user",
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

  val prodAppConfigConstant =
    AppConfig(
      featureSwitches = featureSwitches,
      devIrlFrontendConfig = devIrlFrontendConfig,
      redisConfig = redisConfig,
      kafkaConfig = kafkaConfig,
      postgresqlConfig = containerPostgreSqlConfig,
      serverConfig = appServerConfig
    )
}
