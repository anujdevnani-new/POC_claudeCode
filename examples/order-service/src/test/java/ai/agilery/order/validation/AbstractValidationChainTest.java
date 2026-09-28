package ai.agilery.order.validation;


import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ai.agilery.order.dto.ValidationErrorDto;

@ExtendWith(MockitoExtension.class)
class AbstractValidationChainTest {

    static class TestValidationChain extends AbstractValidationChain<String> {

        TestValidationChain() {
            // No-op for this test
        }
    }

    /**
     * The chain to be tested.
     */
    private TestValidationChain chain;

    /**
     * The handler1 to be added in the chain.
     */
    @Mock
    private ValidationHandler<String> handler1;

    /**
     * The handler2 to be added in the chain.
     */
    @Mock
    private ValidationHandler<String> handler2;

    @BeforeEach
    void setUp() {
        chain = new TestValidationChain();
    }

    @Test
    void testHandlersWithErrors() {
        ValidationErrorDto error1 = new ValidationErrorDto();
        error1.setField("field1");
        error1.setMessage("error1");

        ValidationErrorDto error2 = new ValidationErrorDto();
        error2.setField("field2");
        error2.setMessage("error2");

        Mockito.when(handler1.validate("test")).thenReturn(List.of(error1));
        Mockito.when(handler2.validate("test")).thenReturn(List.of(error2));

        chain.add(handler1);
        chain.add(handler2);

        // Act
        List<ValidationErrorDto> result = chain.validate("test");

        // Assert
        Mockito.verify(handler1).validate("test");
        Mockito.verify(handler2).validate("test");

        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals("field1", result.get(0).getField());
        Assertions.assertEquals("field2", result.get(1).getField());
    }

    @Test
    void testWithNoHandlers() {
        List<ValidationErrorDto> result = chain.validate("test");
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void testHanldersWithNoErrors() {
        Mockito.when(handler1.validate("test")).thenReturn(Collections.emptyList());
        chain.add(handler1);

        List<ValidationErrorDto> result = chain.validate("test");

        Mockito.verify(handler1).validate("test");
        Assertions.assertTrue(result.isEmpty());
    }
}
