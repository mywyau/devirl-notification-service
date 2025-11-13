// // modules/KafkaModule.scala
// package modules

// import cats.effect.*
// import configuration.AppConfig
// import infrastructure.KafkaProducerProvider
// import kafka.*
// import org.typelevel.log4cats.Logger

// final case class KafkaProducers[F[_]](
//   registrationEventProducer: RegistrationEventProducerAlgebra[F]
// )

// object KafkaModule {

//   def make[F[_] : Async : Logger](appConfig: AppConfig): Resource[F, KafkaProducers[F]] =
//     for {
//       // ✅ Use your existing provider
//       producer <- KafkaProducerProvider.make[F](
//         bootstrap = appConfig.kafkaConfig.bootstrapServers,
//         clientId = appConfig.kafkaConfig.clientId,
//         acks = appConfig.kafkaConfig.acks,
//         lingerMs = appConfig.kafkaConfig.lingerMs,
//         retries = appConfig.kafkaConfig.retries
//       )
//       registrationEventProducer = new RegistrationEventProducerImpl[F](appConfig.kafkaConfig.topic.userRegistered, producer)
//     } yield KafkaProducers(registrationEventProducer)
// }
