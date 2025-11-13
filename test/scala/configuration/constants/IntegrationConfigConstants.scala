package configuration.constants

import configuration.models.*
import configuration.AppConfig

object IntegrationConfigConstants {

  val featureSwitches =
    FeatureSwitches(
      useDockerHost = false,
      localTesting = false,
      useCors = false
    )

  val itDevIrlFrontendConfig =
    DevIrlFrontendConfig(
      host = "0.0.0.0",
      port = 3000,
      baseUrl = "http://localhost:3000"
    )

  val itSpecServerConfig =
    ServerConfig(
      host = "127.0.0.1",
      port = 9999
    )

  val itPostgresqlConfig =
    PostgresqlConfig(
      dbName = "dev_notification_test_db",
      dockerHost = "dev-notification-db-it",
      host = "localhost",
      port = 5435,
      username = "dev_notification_test_user",
      password = "turnip",
      maxPoolSize = 42
    )

  val itRedisConfig =
    RedisConfig(
      dockerHost = "redis-test-container",
      host = "localhost",
      port = 6380
    )

  val kafkaConfig =
    KafkaConfig(
      bootstrapServers = "localhost:9092",
      clientId = "devirl-notification-service",
      acks = "all",
      lingerMs = 5,
      retries = 10,
      topic = KafkaTopicConfig(
        "user.registered.v1"
      )
    )

  val integrationAppConfigConstant =
    AppConfig(
      featureSwitches = featureSwitches,
      devIrlFrontendConfig = itDevIrlFrontendConfig,
      redisConfig = itRedisConfig,
      kafkaConfig = kafkaConfig,
      postgresqlConfig = itPostgresqlConfig,
      serverConfig = itSpecServerConfig
    )
}
