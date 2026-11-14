package com.smartats.engine;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.*;

public class XMLTaxonomyParser {

    public static class Skill {
        private final String name;
        private final int weight;
        private final List<String> synonyms = new ArrayList<>();

        public Skill(String name, int weight) {
            this.name = name;
            this.weight = weight;
        }

        public String getName() {
            return name;
        }

        public int getWeight() {
            return weight;
        }

        public List<String> getSynonyms() {
            return Collections.unmodifiableList(synonyms);
        }

        public void addSynonym(String syn) {
            if (syn != null && !syn.trim().isEmpty()) {
                synonyms.add(syn.trim().toLowerCase());
            }
        }
    }

    public static class RoleDefinition {
        private final String id;
        private final String name;
        private final List<Skill> skills = new ArrayList<>();

        public RoleDefinition(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public List<Skill> getSkills() {
            return Collections.unmodifiableList(skills);
        }

        public void addSkill(Skill skill) {
            skills.add(skill);
        }
    }

    private static volatile XMLTaxonomyParser instance;
    private final Map<String, RoleDefinition> roleMap = new LinkedHashMap<>();

    private XMLTaxonomyParser() {
        loadTaxonomy();
    }

    public static XMLTaxonomyParser getInstance() {
        if (instance == null) {
            synchronized (XMLTaxonomyParser.class) {
                if (instance == null) {
                    instance = new XMLTaxonomyParser();
                }
            }
        }
        return instance;
    }

    private void loadTaxonomy() {
        try {
            InputStream in = getClass().getClassLoader().getResourceAsStream("role-skills.xml");
            if (in == null) {
                in = getClass().getClassLoader().getResourceAsStream("WEB-INF/role-skills.xml");
            }
            if (in == null) {
                System.err.println("[XMLTaxonomyParser] Warning: role-skills.xml not found on classpath, loading defaults.");
                loadFallbackTaxonomy();
                return;
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(in);
            doc.getDocumentElement().normalize();

            NodeList roleNodes = doc.getElementsByTagName("role");
            for (int i = 0; i < roleNodes.getLength(); i++) {
                Element roleElem = (Element) roleNodes.item(i);
                String roleId = roleElem.getAttribute("id");
                String roleName = roleElem.getAttribute("name");

                RoleDefinition roleDef = new RoleDefinition(roleId, roleName);

                NodeList skillNodes = roleElem.getElementsByTagName("skill");
                for (int j = 0; j < skillNodes.getLength(); j++) {
                    Element skillElem = (Element) skillNodes.item(j);
                    String skillName = skillElem.getAttribute("name");
                    int weight = 5;
                    try {
                        weight = Integer.parseInt(skillElem.getAttribute("weight"));
                    } catch (NumberFormatException ignored) {}

                    Skill skill = new Skill(skillName, weight);

                    NodeList synNodes = skillElem.getElementsByTagName("synonym");
                    for (int k = 0; k < synNodes.getLength(); k++) {
                        skill.addSynonym(synNodes.item(k).getTextContent());
                    }

                    roleDef.addSkill(skill);
                }

                roleMap.put(roleName.toLowerCase(), roleDef);
            }
            System.out.println("[XMLTaxonomyParser] Loaded " + roleMap.size() + " roles from XML taxonomy successfully.");

        } catch (Exception e) {
            System.err.println("[XMLTaxonomyParser] Error parsing role-skills.xml: " + e.getMessage());
            loadFallbackTaxonomy();
        }
    }

    private void loadFallbackTaxonomy() {
        RoleDefinition web = new RoleDefinition("web_developer", "Web Developer");
        web.addSkill(createSkill("Java", 10, "Core Java", "J2SE"));
        web.addSkill(createSkill("JSP", 8, "Servlets", "Jakarta"));
        web.addSkill(createSkill("JDBC", 8, "SQL Driver", "Database"));
        web.addSkill(createSkill("SQL", 7, "MySQL", "RDBMS"));
        web.addSkill(createSkill("HTML", 5, "HTML5"));
        web.addSkill(createSkill("CSS", 5, "CSS3", "Bootstrap"));
        web.addSkill(createSkill("JavaScript", 7, "JS", "ES6"));
        web.addSkill(createSkill("XML", 6, "DOM"));
        roleMap.put("web developer", web);

        RoleDefinition app = new RoleDefinition("app_developer", "App Developer");
        app.addSkill(createSkill("Android", 10, "Android Studio", "SDK"));
        app.addSkill(createSkill("Kotlin", 9));
        app.addSkill(createSkill("Java", 8));
        app.addSkill(createSkill("Flutter", 8, "Dart"));
        app.addSkill(createSkill("REST API", 7, "JSON", "Retrofit"));
        roleMap.put("app developer", app);

        RoleDefinition video = new RoleDefinition("video_editor", "Video Editor");
        video.addSkill(createSkill("Premiere Pro", 10, "Adobe Premiere"));
        video.addSkill(createSkill("After Effects", 9, "Motion Graphics"));
        video.addSkill(createSkill("DaVinci Resolve", 9));
        video.addSkill(createSkill("Color Grading", 7));
        video.addSkill(createSkill("Audio Sync", 6));
        roleMap.put("video editor", video);
    }

    private Skill createSkill(String name, int weight, String... synonyms) {
        Skill s = new Skill(name, weight);
        for (String syn : synonyms) {
            s.addSynonym(syn);
        }
        return s;
    }

    public RoleDefinition getRoleDefinition(String roleName) {
        if (roleName == null) return null;
        return roleMap.get(roleName.trim().toLowerCase());
    }

    public List<RoleDefinition> getAllRoles() {
        return new ArrayList<>(roleMap.values());
    }
}
