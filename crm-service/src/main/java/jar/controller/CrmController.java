package jar.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jar.model.CustomerForm;
import jar.model.DashboardStats;
import jar.service.CrmDashboardService;
import jar.service.CustomerService;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class CrmController {

    private final CrmDashboardService crmDashboardService;
    private final CustomerService customerService;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("stats", crmDashboardService.getStats());
        model.addAttribute("customers", customerService.getAllCustomers());
        return "index";
    }

    @GetMapping("/customers/new")
    public String newCustomerForm(Model model) {
        model.addAttribute("customer", new CustomerForm());
        model.addAttribute("isEdit", false);
        return "customer-form";
    }

    @PostMapping("/customers")
    public String createCustomer(
            @ModelAttribute CustomerForm customer,
            RedirectAttributes redirectAttributes) {
        try {
            customerService.createCustomer(customer);
            redirectAttributes.addFlashAttribute("message", "고객이 등록되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "등록 실패: " + e.getMessage());
            return "redirect:/customers/new";
        }
        return "redirect:/";
    }

    @GetMapping("/customers/{id}/edit")
    public String editCustomerForm(@PathVariable Long id, Model model) {
        model.addAttribute("customer", customerService.getCustomerForm(id));
        model.addAttribute("isEdit", true);
        return "customer-form";
    }

    @PostMapping("/customers/{id}")
    public String updateCustomer(
            @PathVariable Long id,
            @ModelAttribute CustomerForm customer,
            RedirectAttributes redirectAttributes) {
        try {
            customerService.updateCustomer(id, customer);
            redirectAttributes.addFlashAttribute("message", "고객 정보가 수정되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "수정 실패: " + e.getMessage());
            return "redirect:/customers/" + id + "/edit";
        }
        return "redirect:/";
    }

    @PostMapping("/customers/{id}/delete")
    public String deleteCustomer(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        try {
            customerService.deleteCustomer(id);
            redirectAttributes.addFlashAttribute("message", "고객이 삭제되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "삭제 실패: " + e.getMessage());
        }
        return "redirect:/";
    }

}
