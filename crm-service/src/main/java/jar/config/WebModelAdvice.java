package jar.config;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import lombok.RequiredArgsConstructor;

@ControllerAdvice
@RequiredArgsConstructor
public class WebModelAdvice {

    private final AppProperties appProperties;

    @ModelAttribute("basePath")
    public String basePath() {
        return appProperties.getBasePath();
    }

}
