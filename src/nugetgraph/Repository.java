package nugetgraph;

import java.io.IOException;
import java.util.List;

/** Supplies dependencies independently of graph traversal. */
public interface Repository {
    /** Resolves a name and NuGet version range to a concrete node. */
    PackageRef resolve(String name, String range) throws IOException;

    /** Fetches direct dependencies for a resolved node. */
    List<PackageRef> dependencies(PackageRef node) throws IOException;
}
