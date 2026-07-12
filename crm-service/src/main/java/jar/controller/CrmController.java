package jar.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import jar.model.CustomerListView;
import jar.model.DashboardStats;
import jar.service.CustomerService;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class CrmController {

    private final CustomerService customerService;

    @GetMapping("/")
    public String home(Model model) {
        CustomerListView customerList = customerService.getAllCustomers();
        model.addAttribute("stats", new DashboardStats(customerList.customers().size(), 0, 0, 0));
        model.addAttribute("customers", customerList.customers());
        model.addAttribute("userServiceUnavailable", customerList.userServiceUnavailable());
        return "index";
    }

    @GetMapping("/customers/{id}")
    public String customerDetail(@PathVariable Long id, Model model) {
        model.addAttribute("customer", customerService.getCustomer(id));
        return "customer-detail";
    }

}
