package com.example.social_be.util;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CookieServiceTest {

  private CookieService secureService() {
    CookieService service = new CookieService();
    ReflectionTestUtils.setField(service, "secure", true);
    return service;
  }

  private CookieService relaxedService() {
    CookieService service = new CookieService();
    ReflectionTestUtils.setField(service, "secure", false);
    return service;
  }

  @Test
  void attachAuthCookies_setsExactlyTwoCookies_withProdAttributes() {
    MockHttpServletResponse response = new MockHttpServletResponse();

    secureService().attachAuthCookies(response, "access-tok", 86400, "refresh-tok", 604800);

    List<String> setCookies = response.getHeaders("Set-Cookie");
    assertThat(setCookies).hasSize(2);
    assertThat(setCookies.get(0))
        .contains("token=access-tok")
        .contains("HttpOnly")
        .contains("Secure")
        .contains("SameSite=None")
        .contains("Path=/")
        .contains("Max-Age=86400");
    assertThat(setCookies.get(1))
        .contains("refreshToken=refresh-tok")
        .contains("Max-Age=604800");
  }

  @Test
  void clearAuthCookies_setsZeroMaxAge_forBothCookies() {
    MockHttpServletResponse response = new MockHttpServletResponse();

    secureService().clearAuthCookies(response);

    List<String> setCookies = response.getHeaders("Set-Cookie");
    assertThat(setCookies).hasSize(2);
    assertThat(setCookies.get(0)).contains("token=").contains("Max-Age=0");
    assertThat(setCookies.get(1)).contains("refreshToken=").contains("Max-Age=0");
  }

  @Test
  void whenNotSecure_sameSiteFallsBackToLax_neverNoneWithoutSecure() {
    MockHttpServletResponse response = new MockHttpServletResponse();

    relaxedService().attachAuthCookies(response, "access-tok", 86400, "refresh-tok", 604800);

    List<String> setCookies = response.getHeaders("Set-Cookie");
    assertThat(setCookies).allSatisfy(cookie -> {
      assertThat(cookie).doesNotContain("Secure");
      assertThat(cookie).contains("SameSite=Lax");
    });
  }
}
