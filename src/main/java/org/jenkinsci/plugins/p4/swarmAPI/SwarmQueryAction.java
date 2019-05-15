package org.jenkinsci.plugins.p4.swarmAPI;

import com.google.gson.Gson;
import hudson.Extension;
import hudson.model.RootAction;
import jenkins.branch.BranchSource;
import jenkins.model.Jenkins;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.p4.client.ConnectionHelper;
import org.jenkinsci.plugins.p4.populate.AutoCleanImpl;
import org.jenkinsci.plugins.p4.scm.SwarmScmSource;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@Symbol("swarm_projects")
@Extension
public class SwarmQueryAction implements RootAction {

	@Override
	public String getIconFileName() {
		return null;
	}

	@Override
	public String getDisplayName() {
		return null;
	}

	@Override
	public String getUrlName() {
		return "swarm";
	}

	public void doDynamic(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {

		String path = req.getRestOfPath();

		if (path != null && path.startsWith("/project")) {
			String credentialID = req.getParameter("credential");

			try (ConnectionHelper p4 = new ConnectionHelper(credentialID, null)) {
				SwarmHelper swarm = new SwarmHelper(p4, "4");
				List<String> list = swarm.getProjects();

				Gson gson = new Gson();
				String json = gson.toJson(list);

				PrintWriter out = rsp.getWriter();
				out.write(json);
			} catch (Exception e) {
				throw new IOException(e);
			}
		}

		if (path != null && path.startsWith("/create")) {
			String credentialID = req.getParameter("credential");
			String project = req.getParameter("project");
			String name = req.getParameter("name");

			if (name == null || name.isEmpty()) {
				name = project;
			}

			try (ConnectionHelper p4 = new ConnectionHelper(credentialID, null)) {
				SwarmHelper swarm = new SwarmHelper(p4, "4");

				String format = "jenkins-${NODE_NAME}-${JOB_NAME}";
				SwarmScmSource source = new SwarmScmSource(credentialID, null, format);
				source.setProject(project);
				source.setSwarm(swarm);
				source.setPopulate(new AutoCleanImpl());

				WorkflowMultiBranchProject multi = Jenkins.getInstance().createProject(WorkflowMultiBranchProject.class, name);
				multi.getSourcesList().add(new BranchSource(source));

				multi.scheduleBuild2(0);
			} catch (Exception e) {
				throw new IOException(e);
			}
		}
	}
}