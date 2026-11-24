package io.github.jsshin8128.order.customer;

import io.github.jsshin8128.order.Order;
import io.github.jsshin8128.order.OrderRepository;
import io.github.jsshin8128.order.OrderSummary;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

	private final CustomerRepository customerRepository;
	private final OrderRepository orderRepository;

	public CustomerController(CustomerRepository customerRepository, OrderRepository orderRepository) {
		this.customerRepository = customerRepository;
		this.orderRepository = orderRepository;
	}

	@GetMapping("/{id}")
	public Customer findById(@PathVariable long id) {
		return customerRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
	}

	@GetMapping("/{id}/order-summary")
	public OrderSummary summarizeOrders(@PathVariable long id) {
		return orderRepository.summarizeByCustomerId(id);
	}

	@GetMapping("/{id}/orders/latest")
	public Order findLatestOrder(@PathVariable long id) {
		return orderRepository.findLatestByCustomerId(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
	}

}
