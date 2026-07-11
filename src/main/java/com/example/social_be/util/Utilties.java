package com.example.social_be.util;

import lombok.Data;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;

@Data
public class Utilties {
  public String dayTimeFormat() {
    Date date = new Date();
    SimpleDateFormat sdf1 = new SimpleDateFormat("MM-dd-yyyy kk:mm:ss");
    String strDate = sdf1.format(date);
    strDate = sdf1.format(date);
    return strDate;
  }

  public static String extractUsername(String email) {
    if (email == null || !email.contains("@")) {
      throw new IllegalArgumentException("Invalid email address");
    }
    return email.substring(0, email.indexOf("@"));
  }

  /**
   * Builds a Mongo $regex pattern that literally, case-sensitively matches
   * strings starting with {@code input}. Special regex characters in
   * {@code input} are escaped (via {@link Pattern#quote}) so they can never
   * be interpreted as regex syntax, and anchoring with {@code ^} keeps the
   * query index-friendly (a prefix scan) instead of an unanchored full scan.
   */
  public static String anchoredLiteralPrefix(String input) {
    return "^" + Pattern.quote(input);
  }
}
