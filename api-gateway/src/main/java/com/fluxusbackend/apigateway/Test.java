import org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions;
import java.lang.reflect.Method;
public class Test {
    public static void main(String[] args) {
        for (Method m : HandlerFunctions.class.getMethods()) {
            System.out.println(m.getName() + " : " + m.getParameterCount());
            for (Class<?> p : m.getParameterTypes()) {
                System.out.println("  " + p.getName());
            }
        }
    }
}
