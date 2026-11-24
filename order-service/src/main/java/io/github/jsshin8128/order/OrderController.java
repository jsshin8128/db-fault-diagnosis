package io.github.jsshin8128.order;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

	private final OrderRepository orderRepository;

	public OrderController(OrderRepository orderRepository) {
		this.orderRepository = orderRepository;
	}

	@GetMapping
	public List<Order> findByCustomerId(@RequestParam long customerId) {
		return orderRepository.findByCustomerId(customerId);
	}

	@GetMapping("/{id}")
	public Order findById(@PathVariable long id) {
		return orderRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
	}

	@GetMapping("/recent")
	public List<Order> findRecent() {
		return orderRepository.findRecent();
	}

	@GetMapping("/count")
	public Map<String, Long> countByOrderedOn(
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate orderedOn) {
		return Map.of("count", orderRepository.countByOrderedOn(orderedOn));
	}

	@PatchMapping("/{id}/status")
	public ResponseEntity<Void> updateStatus(@PathVariable long id, @RequestBody OrderStatusUpdate update) {
		if (!orderRepository.updateStatus(id, update.status())) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
		return ResponseEntity.noContent().build();
	}

}
