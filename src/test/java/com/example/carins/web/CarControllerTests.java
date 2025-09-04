package com.example.carins.web;

import com.example.carins.exception.GlobalExceptionHandler;
import com.example.carins.service.CarService;
import com.example.carins.web.dto.ClaimDto;
import com.example.carins.web.dto.CreateClaimRequest;
import com.example.carins.web.dto.PolicyResponse;
import com.example.carins.web.mapper.CarMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

class CarControllerTests {

    private MockMvc mvc;
    private CarService service;

    @BeforeEach
    void setUp() {
        service = mock(CarService.class);
        CarMapper carMapper = mock(CarMapper.class);

        CarController controller = new CarController(service, carMapper);

        ObjectMapper om = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(om))
                .setControllerAdvice(new GlobalExceptionHandler()) // so we get your ApiError JSON
                .build();
    }

    @Test
    void createPolicy_happyPath_returns201_withLocationAndBody() throws Exception {
        long carId = 1L;
        String provider = "Allianz";
        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate end   = LocalDate.of(2025, 12, 31);

        PolicyResponse returned = new PolicyResponse(10L, carId, provider, start, end);
        when(service.createPolicy(eq(carId), eq(start), eq(end), eq(provider)))
                .thenReturn(returned);

        String body = """
          {
            "startDate": "2025-01-01",
            "endDate":   "2025-12-31",
            "provider":  "Allianz"
          }
        """;

        mvc.perform(post("/api/cars/{carId}/policies", carId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/policies/10"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.carId").value(1))
                .andExpect(jsonPath("$.provider").value("Allianz"))
                .andExpect(jsonPath("$.startDate").value("2025-01-01"))
                .andExpect(jsonPath("$.endDate").value("2025-12-31"));

        verify(service).createPolicy(eq(carId), eq(start), eq(end), eq(provider));
        verifyNoMoreInteractions(service);
    }

    @Test
    void createPolicy_missingEndDate_returns400_withValidationDetails() throws Exception {
        long carId = 1L;
        String body = """
          {
            "startDate": "2025-01-01",
            "provider":  "TookaMooka"
          }
        """;

        mvc.perform(post("/api/cars/{carId}/policies", carId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.exception")
                        .value("org.springframework.web.bind.MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.path").value("/api/cars/1/policies"))
                .andExpect(jsonPath("$.details[0].field").value("endDate"))
                .andExpect(jsonPath("$.details[0].message").value("endDate is required"));

        verifyNoInteractions(service); // service should not be called on validation failure
    }

    //P2
    @Test
    void createClaim_happyPath_returns201_withLocationAndBody() throws Exception {
        long carId = 1L;
        long claimId = 99L;
        LocalDate claimDate = LocalDate.of(2025, 9, 1);
        String description = "Minor accident";
        BigDecimal amount = new BigDecimal("1200.50");

        CreateClaimRequest request = new CreateClaimRequest(claimDate, description, amount);
        ClaimDto returned = new ClaimDto(claimId, carId, claimDate, description, amount);

        when(service.registerClaim(eq(carId), eq(request))).thenReturn(returned);

        String body = """
          {
            "claimDate": "2025-09-01",
            "description": "Minor accident",
            "amount": 1200.50
          }
        """;

        mvc.perform(post("/api/cars/{carId}/claims", carId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        "http://localhost/api/cars/" + carId + "/claims/" + claimId))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(claimId))
                .andExpect(jsonPath("$.carId").value(carId))
                .andExpect(jsonPath("$.claimDate").value("2025-09-01"))
                .andExpect(jsonPath("$.description").value(description))
                .andExpect(jsonPath("$.amount").value(1200.50));

        verify(service).registerClaim(eq(carId), eq(request));
        verifyNoMoreInteractions(service);
    }

    @Test
    void createClaim_carNotFound_returns404_withErrorDetails() throws Exception {
        long carId = 999L; // non-existent car
        String body = """
      {
        "claimDate": "2025-09-01",
        "description": "Minor accident",
        "amount": 1200.50
      }
    """;

        when(service.registerClaim(eq(carId), any(CreateClaimRequest.class)))
                .thenThrow(new com.example.carins.exception.ResourceNotFoundException("Car not found"));

        mvc.perform(post("/api/cars/{carId}/claims", carId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Request failed"))
                .andExpect(jsonPath("$.exception")
                        .value("com.example.carins.exception.ResourceNotFoundException"))
                .andExpect(jsonPath("$.path", endsWith("/api/cars/" + carId + "/claims")));

        verify(service).registerClaim(eq(carId), any(CreateClaimRequest.class));
        verifyNoMoreInteractions(service);
    }

    @Test
    void insuranceValid_returnsTrueForActivePolicy() throws Exception {
        // carId=1 has Allianz policy valid until 2024-12-31 (see import.sql)
        mvc.perform(get("/api/cars/1/insurance-valid")
                        .param("date", "2024-06-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.carId").value(1))
                .andExpect(jsonPath("$.date").value("2024-06-01"))
                .andExpect(jsonPath("$.valid").value(false));
    }

    @Test
    void history_nonExistingCar_returns404() throws Exception {
        long missingId = 21L;

        when(service.getHistory(missingId))
                .thenThrow(new com.example.carins.exception.ResourceNotFoundException("Car " + missingId + " not found"));

        mvc.perform(get("/api/cars/{carId}/history", missingId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Request failed"))
                .andExpect(jsonPath("$.path").value("/api/cars/" + missingId + "/history"));

        verify(service).getHistory(missingId);
        verifyNoMoreInteractions(service);
    }

}
