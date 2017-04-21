package scot.mygov.search;

import org.junit.Test;

import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ErrorHandlerTest {

    private final ErrorHandler handler = new ErrorHandler();

    @Test
    public void unexpectedExceptionShouldNotReturnStackTrace() {
        RuntimeException exception = new RuntimeException("Error handler test");
        Response response = handler.toResponse(exception);
        assertEquals(500, response.getStatus());
        String body = (String) response.getEntity();
        assertFalse(body.contains(ErrorHandlerTest.class.getName()));
    }

}
