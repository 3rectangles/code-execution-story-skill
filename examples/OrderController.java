// Illustrative input for the code-execution-story skill.
// Not a complete compilable service — it exists to show what the skill traces.

@RestController
class OrderController {
    private final OrderService orderService;

    OrderController(OrderService s) {
        this.orderService = s;
    }

    @PostMapping("/orders")
    Order createOrder(@RequestBody CreateOrderRequest request) {
        return orderService.createOrder(request);
    }
}

@Service
class OrderService {
    private final InventoryService inventory;
    private final PaymentService payments;
    private final OrderRepository repository;
    private final KafkaTemplate<String, Object> kafka;

    // ... constructor injection elided ...

    Order createOrder(CreateOrderRequest request) {
        validate(request);
        Order order = buildOrder(request);
        order.setTotal(calculateTotal(order.getItems()));
        inventory.reserve(order.getItems());
        payments.pay(order.getTotal());
        Order saved = repository.save(order);
        kafka.send("OrderCreated", new OrderCreated(saved.getId()));
        return saved;
    }

    private void validate(CreateOrderRequest r) { /* ... */ }
    private Order buildOrder(CreateOrderRequest r) { /* ... */ }
    private Money calculateTotal(List<Item> items) { /* ... */ }
}
