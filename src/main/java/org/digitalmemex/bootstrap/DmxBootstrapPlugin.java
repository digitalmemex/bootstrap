package org.digitalmemex.bootstrap;

import de.deepamehta.core.Topic;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.Inject;
import de.deepamehta.core.service.Transactional;
import de.deepamehta.plugins.files.service.FilesService;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.io.*;
import java.util.logging.Logger;

@Path("/dmx")
@Produces("application/json")
public class DmxBootstrapPlugin extends PluginActivator {

    private Logger logger = Logger.getLogger(getClass().getName());
    @Inject
    private FilesService filesService;

    @GET
    @Path("/repository/{name}")
    public DmxRepository getRepository(@PathParam("name") String name) {
        logger.info("repository request " + name + " topic");

        Topic repoName = dms.getTopic("dmx.repository.name", new SimpleValue(name));
        if (repoName == null) {
            Response response = Response.status(404).entity("repository name " + name + " not found").build();
            throw new WebApplicationException(response);
        }

        Topic repo = repoName.getRelatedTopic("dm4.core.composition", "dm4.core.child", "dm4.core.parent", "dmx.repository");
        if (repo == null) {
            logger.warning("repository parent topic of name " + name + " not found");
            Response response = Response.status(404).entity("repository topic " + name + " not found").build();
            throw new WebApplicationException(response);
        }

        return new DmxRepository(repo);
    }

    @GET
    @Path("/application/{name}")
    @Produces("text/html")
    public InputStream getApplicationIndex(@PathParam("name") String name) {
        logger.info("request application " + name + " index");

        DmxRepository repo = getRepository(name);
        if (filesService == null) {
            throw new RuntimeException("file service plugin is not available");
        }

        String index = "/dmx/" + repo.getName() + "/index.html";
        File file = filesService.getFile(index);
        try {
            return new FileInputStream(file);
        } catch (FileNotFoundException e) {
            String message = "index file  " + index + " not found";
            Response response = Response.status(404).entity(message).build();
            throw new WebApplicationException(response);
        }
    }

    @GET
    @Produces("text/html")
    public InputStream index() {
        // TODO use plugin configuration
        return getApplicationIndex("repository-manager");
    }

    private File getRepositoryDirectory(DmxRepository repo) {
        if (filesService == null) {
            throw new RuntimeException("file service plugin is not available");
        }

        if (repo.isInstalled()) { // return .git path
            return filesService.getFile("/dmx/" + repo.getName() + "/.git");
        } else { // repo.isConfigured()? create and return clone root
            filesService.createFolder("dmx/" + repo.getName(), "/");
            return filesService.getFile("/dmx/" + repo.getName());
        }
    }

    @GET
    @Path("/repository/{name}/pull")
    @Transactional
    public DmxRepository getRepositoryPull(@PathParam("name") String name) throws IOException, GitAPIException {
        logger.info("pull request " + name);
        DmxRepository repo = getRepository(name);
        Repository gitRepo = new FileRepositoryBuilder().readEnvironment().findGitDir()
                .setGitDir(getRepositoryDirectory(repo)).build();
        PullResult result = new Git(gitRepo).pull().call();
        repo.setBranch(gitRepo.getFullBranch());
        repo.setHead(gitRepo.resolve("HEAD").getName());
        return repo;
    }

    @GET
    @Path("/repository/{name}/clone")
    @Transactional
    public DmxRepository cloneRepository(@PathParam("name") String name) throws GitAPIException, IOException {
        logger.info("clone request " + name);
        DmxRepository repo = getRepository(name);
        File path = getRepositoryDirectory(repo);
        Git git = Git.cloneRepository().setURI(repo.getUri()).setDirectory(path)
                .setBranch("master").setRemote("origin")
                .setBare(false).setNoCheckout(false).call();
        Repository gitRepo = git.getRepository();
        repo.setBranch(gitRepo.getBranch());
        repo.setHead(gitRepo.resolve("HEAD").getName());
        repo.setStatusInstalled();
        return repo;
    }

}
