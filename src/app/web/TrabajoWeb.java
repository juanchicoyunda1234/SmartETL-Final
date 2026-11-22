package app.web;

import java.util.function.Consumer;

public interface TrabajoWeb {

    Object hacer(Consumer<String> avisar) throws Exception;
}
