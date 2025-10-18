package com.example;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController
@RequestMapping("spring/api/items")
public class ItemController {
  private final List<Map<String,Object>> items = Collections.synchronizedList(new ArrayList<>());
  @GetMapping
  public List<Map<String,Object>> list(){ return items; }
  @PostMapping
  public Map<String,Object> create(@RequestBody Map<String,Object> body) {
    body.put("id", UUID.randomUUID().toString());
    items.add(body);
    // In real app: write to Couchbase and produce Kafka message (include correlation_id)
    return body;
  }
}
