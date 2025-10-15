import static org.hamcrest.CoreMatchers.notNullValue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.restassured.RestAssured;

public class PatientIntegrationTest {

	@BeforeAll
	static void setUp() {
		RestAssured.baseURI = "http://localhost:4004";
	}

	@Test
	public void shouldReturnPatientsWithValidToken() {
		String loginPayload = """
					{
						"email": "testuser@test.com",
						"password": "password123"
					}
				""";

		String token = RestAssured.given().contentType("application/json").body(loginPayload).when().post("auth/login")
				.then().statusCode(200).extract().jsonPath().get("token");

		RestAssured.given().header("Authorization", "Bearer " + token).when().get("/api/patients").then()
				.statusCode(200).body("patients", notNullValue());
	}

}
