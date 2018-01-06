package com.kodedu.cloudterm.controller;

import com.kodedu.cloudterm.service.TerminalService;
import com.kodedu.cloudterm.websocket.TerminalSocket;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class HomeController {

    @RequestMapping("/terminal")

    public String home(String wsut, Model model) {
        model.addAttribute("wsut", wsut);
        return "home";
    }

    @RequestMapping("/command")
    @ResponseBody
    public String command(String sessionId, String command) {
        TerminalService service = TerminalSocket.getTerminal(sessionId);
        if (service != null) {
            service.onTerminalReady(command);
//            return service.popResult();
        }
        return "FAILED";
    }
}
