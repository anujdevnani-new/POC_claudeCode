package ai.agilery.order.utils;

import org.junit.jupiter.api.BeforeEach;

import ai.agilery.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class IntegrationTestDataHelper {

    private final OrderRepository orderRepository;

    @BeforeEach
    protected void tearDown() {
        orderRepository.deleteAll();
    }
}
