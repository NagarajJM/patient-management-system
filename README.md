# Patient Management System
<img width="1734" height="973" alt="patient-management" src="https://github.com/user-attachments/assets/1e3c103d-372a-4408-9940-586d2efa25bf" />

## Microservices
- #### [API Gateway](#api-gateway-1)
- #### [Auth Service](#auth-service-1)
- #### [Patient Service](#patient-service-1)
- #### [Billing Service](#billing-service-1)
- #### [Analytics Service](#analytics-service-1)

## Inter-Service Communication
- REST
- gRPC
- Kafka

## Other Packages
- [Integration Tests](#integration-tests)
- [Infrastructure](#infrastructure)

### API Gateway
Utilizes **JwtValidationGatewayFilterFactory.class** and to build the WebClient,
it takes AUTH_SERVICE_URL environment variable and will call "/validate" uri of [Auth Service](#auth-service) with "Bearer 123" token.
The JWT needs to be passed in the Authorization Header of every client request except "/login".
To get a valid JWT with expiration of 10 hours, call "/login" POST request of [Auth Service](auth-service).
It conveniently uses 2 application.yml files, one for the docker setup (application.yml),
another for the localstack setup, which is an aws emulator (application-prod.yml), you can choose it by using SPRING_PROFILES_ACTIVE environment variable.

### Auth Service
It handles 2 endpoints "/login" and "/validate". To login you need to POST LoginRequestDTO which should contain email, password.
It will call UserService to find the user by email and will pass it to **security.crypto.password.PasswordEncoder** to verify.
If not verified you will get ResponseEntity with HttpStatus.UNAUTHORIZED, otherwise, it will call an custom JwtUtil class of util package.
In JwtUtil, you will pass JWT_SECRET as an environment variable, which will be passed to the Keys.hmacShaKeys to generate a secret key,
which will be utilized to generate a returned token. Every subsequent request will pass this JWT and will be verified in this service.

### Patient Service
It has 10 packages logically divided as controller, model, service, repository, dto, mapper, validators, exception, grpc, kafka.
It contains data.sql in the resources folder to populate the postgreSQL database. You can also find commented In-Memory database configuration of H2 database.
There is a **Validation Group** to apply different validation rules for the same DTO. When you try to create patient with the same email,
EmailAlreadyExistsException.class will be thrown, which will be handled by the @ControllerAdvice of GlobalExceptionHandler, which will use slf4j.LoggerFactory to
log the error and will return ResponseEntity.badRequest().body(errors); During creation of Patient, it will also call [createBillingAccount](#billing-service) service of
BillingServiceGrpcClient.class, which is created with the help of .proto file to pass id, name and email. It will use BILLING_SERVICE_ADDRESS and
BILLING_SERVICE_GRPC_PORT environment variables to create a blockingStub. After creation of Patient, it will call sendEvent(Patient patient) of KafkaProducer
to send an event with the help of KafkaTemplate.

### Billing Service
It has gRPC server which is achieved with @GrpcService and extending BillingServiceImplBase which is created with the help of .proto file to receive BillingRequest and
to return BillingResponse. When [Patient Service](#patient-service) calls createBillingAccount, it is calling this method. Inside this method, as soon as it is called,
it will be logged using slf4j.LoggerFactory and in return BillingResponse will be returned.

### Analytics Service
It has KafkaConsumer.class which consumes event. It will receive byte[] which is configured in the application.properties
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer and
spring.kafka.consumer.value-deserializer=org.apache.kafka.common.serialization.ByteArrayDeserializer. It will log the info using log.info(), which uses slf4j.Logger.
During parsing the event, it will call parseFrom(event) which is created with the help of .proto file and it will throw InvalidProtocolBufferException.

### Integration Tests
It conducts tests by calling shouldReturnOKWithValidToken(), shouldReturnUnauthorizedOnInvalidLogin(). It will @BeforeAll to setUp() by setting RestAssured.baseURI and
asserting statusCode, token. It will also assert a negative test case by passing invalid credentials and asseting statusCode. It will assert the contents by using 
notNullValue() when GET Request has been made with valid credentials.

### Infrastructure
It uses Infrastructure as code (IaC) to create a stack of this project. It uses Vpc, Cluster, mskCluster, DatabaseInstance, etc. of awscdk to define all the configuration.
It will create CloudFormation Template, with the help of that, all the microservices can be made to run.

#### Credits
This project was created as a learning exercise based on a tutorial.
[Build & Deploy a Production-Ready Patient Management System with Microservices: Java Spring Boot AWS](https://youtu.be/tseqdcFfTUY?si=gF1sZ9WMmVYElbmi) by
[Chris Blakely](https://youtube.com/@chrisblakely?si=kYdgnnFwZPzgfLMl)
