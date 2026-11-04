package vn.ehoso.dossier.service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import vn.ehoso.security.ApiException.FieldProblem;

/** Checks form data against the procedure's schema (TK-08). */
@Component
public class FormValidator {

  /** Returns one problem per invalid field. */
  @SuppressWarnings("unchecked")
  public List<FieldProblem> validate(Map<String, Object> schema, Map<String, Object> data) {
    List<FieldProblem> problems = new ArrayList<>();
    List<Map<String, Object>> fields =
        (List<Map<String, Object>>) schema.getOrDefault("fields", List.of());
    for (Map<String, Object> field : fields) {
      String key = (String) field.get("key");
      Object value = data == null ? null : data.get(key);
      String text = value == null ? "" : value.toString().trim();
      if (text.isEmpty()) {
        if (Boolean.TRUE.equals(field.get("required"))) {
          problems.add(new FieldProblem(key, field.get("label") + " là bắt buộc."));
        }
        continue;
      }
      String message = check(field, text);
      if (message != null) {
        problems.add(new FieldProblem(key, message));
      }
    }
    return problems;
  }

  @SuppressWarnings("unchecked")
  private String check(Map<String, Object> field, String text) {
    String type = (String) field.get("type");
    Object label = field.get("label");
    switch (type) {
      case "number":
        return text.matches("[1-9]\\d{0,5}") ? null : label + " phải là số dương.";
      case "date":
        try {
          LocalDate.parse(text);
          return null;
        } catch (DateTimeParseException ex) {
          return label + " không đúng định dạng ngày.";
        }
      case "select":
        List<String> options = (List<String>) field.getOrDefault("options", List.of());
        return options.contains(text) ? null : label + " không hợp lệ.";
      default:
        return text.length() <= 500 ? null : label + " quá dài.";
    }
  }
}
