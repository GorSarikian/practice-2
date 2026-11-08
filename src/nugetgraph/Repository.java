package nugetgraph;

import java.io.IOException;
import java.util.List;

public interface Repository {
    public PackageRef resolve(String name, String range) throws IOException;

    public List<PackageRef> dependencies(PackageRef node) throws IOException;
}
