package nugetgraph;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public final class Nuspec {
    public record Requirement(String name, String range) { }

    private Nuspec() { }

    public static List<Requirement> read(String xml, Framework framework) throws IOException {
        Element dependencies = document(xml);
        if (dependencies == null) { return List.of(); }
        List<Element> groups = children(dependencies, "group");
        if (groups.isEmpty()) { return requirements(dependencies); }
        Element selected = null;
        int bestScore = -1;
        for (Element group : groups) {
            int score = framework.score(group.getAttribute("targetFramework"));
            if (score > bestScore) { bestScore = score; selected = group; }
        }
        if (selected == null) { throw new IOException("No compatible dependency group in nuspec"); }
        return requirements(selected);
    }

    private static Element document(String xml) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            var bytes = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
            var builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new org.xml.sax.helpers.DefaultHandler() {
                @Override
                public void fatalError(org.xml.sax.SAXParseException exception) throws org.xml.sax.SAXException {
                    throw exception;
                }
            });
            var root = builder.parse(bytes).getDocumentElement();
            var nodes = root.getElementsByTagNameNS("*", "dependencies");
            return nodes.getLength() == 0 ? null : (Element) nodes.item(0);
        } catch (Exception exception) { throw new IOException("Invalid nuspec: " + exception.getMessage(), exception); }
    }

    private static List<Requirement> requirements(Element parent) throws IOException {
        List<Requirement> result = new ArrayList<>();
        for (Element dependency : children(parent, "dependency")) {
            String name = dependency.getAttribute("id");
            if (!name.matches("[A-Za-z0-9_][A-Za-z0-9_.-]*")) { throw new IOException("Invalid dependency id"); }
            String range = dependency.getAttribute("version");
            new VersionRange(range);
            result.add(new Requirement(name, range));
        }
        return List.copyOf(result);
    }

    private static List<Element> children(Element parent, String name) {
        List<Element> result = new ArrayList<>();
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element element && name.equals(element.getLocalName())) { result.add(element); }
        }
        return result;
    }
}
