package ai.agilery.example.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Home redirection to OpenAPI api documentation.
 */
@Controller
public class HomeController {

    /**
     * Handles HTTP GET requests for the root URL ("/").
     *
     * This method redirects the client to the Swagger UI page. When a user
     * navigates to the root of the application, they are automatically redirected
     * to the Swagger documentation page located at "swagger-ui.html".
     *
     * @return a string representing the redirection URL to the Swagger UI page.
     */
    @GetMapping("/")
    public String index() {
        return "redirect:swagger-ui.html";
    }

}
