import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import com.typesafe.config.ConfigFactory

class DepositsApiSimulations_1_1 extends Simulation {

  val conf = ConfigFactory.load();
  val url = conf.getString("HOST");
  val httpProtocol = http
    .baseUrl(url)
    .acceptHeader(" application/vnd.socash.deposit.v1.1+json")
    .acceptEncodingHeader("gzip, deflate")

  val userCount = Integer.getInteger("Users", 1).toInt
  val testDuration = Integer.getInteger("Duration", 10).toInt

  before {
    println(s"Running test with ${userCount} users")
    println(s"Running test with ${testDuration} seconds")
    println(s"Running test with host ${url}")
  }

   def getDepositCode() = {
    doWhileDuring(session => !session("depositStatus").as[String].equals("PENDING"), session => 60.seconds) {
      exec(http("get deposit code")
      .get("/api/partner/deposits/${depositReferenceId}")
      .header("Authorization", "Bearer ${accessToken}")
      .header("content-type", "application/vnd.socash.deposit.v1.1+json")
      .check(jsonPath("$.code").find.saveAs("depositCode"))
      .check(jsonPath("$.status").find.saveAs("depositStatus"))
      ).pause(2)}
  }
  
  val scn = scenario("DepositSimulation with version 1.1")
    .exec(http("create token")
      .post("/api/partner/deposits/token")
      .header("content-type", "application/vnd.socash.deposit.v1.1+json")
      .body(RawFileBody("version_1.1/create_token.json"))
      .check(jsonPath("$.accessToken").find.saveAs("accessToken"))
    )

    .exec(http("create deposit")
      .post("/api/partner/deposits/deposit-network/43BD3545-FAFC-497F-A575-41B10AD917EB/deposit")
      .header("Authorization", "Bearer ${accessToken}")
      .header("content-type", "application/vnd.socash.deposit.v1.1+json")
      .body(RawFileBody("version_1.1/create_deposits.json"))
      .check(jsonPath("$.depositReferenceId").find.saveAs("depositReferenceId"))
    )

    .exec(getDepositCode())

    .exec(http("cashpoint token")
      .post("/api/cashpoint/token")
      .body(RawFileBody("version_1.1/create_cashpoint_token.json"))
      .header("content-type", "application/json")
      .check(jsonPath("$..authToken").find.saveAs("cashpointAuthToken"))
    )

    .exec(http("complete deposit")
      .put("/api/cashpoint/deposits/${depositReferenceId}/completed")
      .body(ElFileBody("version_1.1/accept_deposit.json"))
      .header("content-type", " application/vnd.socash.deposit.v1.1+json")
      .header("Authorization", "Bearer ${cashpointAuthToken}")
      .check(jsonPath("$.status").is("CASH_DEPOSITED"))
    )
    
  setUp(scn.inject(constantConcurrentUsers(userCount).during(testDuration))).protocols(httpProtocol)
  // setUp(scn.inject(atOnceUsers(userCount))).protocols(httpProtocol)
}
