package com.symphony.devsol.config;

import static jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.symphony.bdk.core.auth.jwt.UserClaim;
import com.symphony.bdk.workflow.api.v1.dto.VersionedWorkflowView;
import com.symphony.bdk.workflow.management.WorkflowManagementService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccessFilter extends OncePerRequestFilter {
  private final Pattern idPattern = Pattern.compile("^id: ([\\w\\-]+)");
  private final Pattern wfPattern = Pattern.compile("/v1/workflows/([\\w\\-]+)");
  private final WorkflowManagementService managementService;
  @Value("${wdk.studio.admins:}")
  private List<Long> admins;

  @Override
  protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                  @NonNull FilterChain chain) throws ServletException, IOException {
    UserClaim user = (UserClaim) request.getAttribute("user");
    if (!List.of("GET", "OPTIONS").contains(request.getMethod())
        && !List.of("mgmt-token", "webhook").contains(user.getUsername())) {
      long userId = user.getId();
      String workflowId = null;

      Matcher matcher = wfPattern.matcher(request.getServletPath());
      if (matcher.find()) {
        workflowId = matcher.group(1);
      }

      if (workflowId == null && List.of("POST", "PUT").contains(request.getMethod())) {
        // Check that caller matches declared author, or is an admin
        byte[] authorBytes = request.getPart("createdBy").getInputStream().readAllBytes();
        long authorId = Long.parseLong(new String(authorBytes, UTF_8));
        if (!admins.contains(userId) && userId != authorId) {
          response.sendError(SC_UNAUTHORIZED, "Your identity does not match the provided author");
          return;
        }

        byte[] swadlBytes = request.getPart("swadl").getInputStream().readAllBytes();
        String swadl = new String(swadlBytes, UTF_8);
        Matcher idMatcher = idPattern.matcher(swadl);
        if (idMatcher.find()) {
          workflowId = idMatcher.group(1);
        }
      }

      // Check that caller owns the workflow
      VersionedWorkflowView workflow = managementService.get(workflowId).orElse(null);
      long ownerId = workflow == null ? 0L : workflow.getCreatedBy();
      if (ownerId > 0L && !admins.contains(userId) && ownerId != userId) {
        response.sendError(SC_UNAUTHORIZED, "You are not allowed to modify this workflow");
        return;
      }
    }
    chain.doFilter(request, response);
  }
}
