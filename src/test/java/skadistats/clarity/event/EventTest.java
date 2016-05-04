package skadistats.clarity.event;

import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class EventTest {
    Set<EventListener<?>> mockEventListenerSet;
    EventListener<?> mockEventListener1;
    EventListener<?> mockEventListener2;
    Event event;

    @Before
    public void setUp() throws Exception {
        mockEventListener1 = mock(EventListener.class);
        mockEventListener2 = mock(EventListener.class);
        mockEventListenerSet = new HashSet<EventListener<?>>() {{
            add(mockEventListener1);
            add(mockEventListener2);
        }};
    }

    @Test
    public void isListenedTo_hasListeners() throws Exception {
        event = new Event(mockEventListenerSet);
        assertTrue(event.isListenedTo());
    }


    @Test
    public void isListenedTo_hasNoListeners() throws Exception {
        event = new Event(new HashSet<EventListener>());
        assertFalse(event.isListenedTo());
    }

    @Test
    public void raise() throws Throwable {
        Object[] fakeArgs = new Object[]{};
        when(mockEventListener1.isInvokedForArguments(fakeArgs)).thenReturn(true);
        when(mockEventListener2.isInvokedForArguments(fakeArgs)).thenReturn(false);

        doNothing().when(mockEventListener1).invoke(fakeArgs);

        event = new Event(mockEventListenerSet);
        event.raise(fakeArgs);

        verify(mockEventListener1, times(1)).invoke(fakeArgs);
    }

    @Test
    public void raise_rethrownException() throws Throwable {
        Object[] fakeArgs = new Object[]{};
        Throwable fakeThrowable = new Throwable("test throwable");
        when(mockEventListener1.isInvokedForArguments(fakeArgs)).thenReturn(true);
        when(mockEventListener2.isInvokedForArguments(fakeArgs)).thenReturn(false);

        doThrow(fakeThrowable).when(mockEventListener1).invoke(fakeArgs);

        event = new Event(mockEventListenerSet);
        try {
            event.raise(fakeArgs);
            fail("should have thrown exception");
        } catch(RuntimeException exception){
            assertEquals(fakeThrowable, exception.getCause());
        }
    }
}