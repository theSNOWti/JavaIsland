package com.javaisland.template;

import com.javaisland.repo.PlayerVarRepository;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StarterCodeTemplate {

  // {{playerVar:key|fallback}}
  private static final Pattern PLAYER_VAR =
      Pattern.compile("\\{\\{\\s*playerVar\\s*:\\s*([A-Za-z_][A-Za-z0-9_]*)\\s*(?:\\|\\s*([^}]*?)\\s*)?\\}\\}");

  public static String resolve(String code, long playerId, PlayerVarRepository playerVarRepo) {
    if (code == null) return null;
    if (playerId <= 0) return code;

    Matcher m = PLAYER_VAR.matcher(code);
    StringBuffer sb = new StringBuffer();

    while (m.find()) {
      String key = m.group(1);
      String fallback = m.group(2);

      String value = playerVarRepo.findValue(playerId, key);
      if (value == null || value.isBlank()) value = (fallback == null ? "" : fallback);

      // Quote replacement to avoid matcher treating $ or \ specially
      m.appendReplacement(sb, Matcher.quoteReplacement(value));
    }
    m.appendTail(sb);

    return sb.toString();
  }

  private StarterCodeTemplate() {}
}