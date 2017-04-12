package controllers;

import ch.heigvd.iict.ser.imdb.models.Role;
import com.thoughtworks.xstream.XStream;
import models.*;
import org.jdom2.*;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import views.MainGUI;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

//import org.dom4j.*;

public class ControleurXMLCreation {

    //private ControleurGeneral ctrGeneral;
    private static MainGUI mainGUI;
    private ORMAccess ormAccess;

    private GlobalData globalData;

    public ControleurXMLCreation(ControleurGeneral ctrGeneral, MainGUI mainGUI, ORMAccess ormAccess) {
        //this.ctrGeneral=ctrGeneral;
        ControleurXMLCreation.mainGUI = mainGUI;
        this.ormAccess = ormAccess;
    }

    public void createXML() {
        new Thread() {
            public void run() {
                mainGUI.setAcknoledgeMessage("Creation XML... WAIT");
                long currentTime = System.currentTimeMillis();
                try {
                    globalData = ormAccess.GET_GLOBAL_DATA();
                    Calendar cal = Calendar.getInstance();
                    DateLabelFormatter dfl = new DateLabelFormatter();
                    globalData = ormAccess.GET_GLOBAL_DATA();
                    Element Projections = new Element("Projections");
                    // Parcourt les projection saisies par l'utilisateur
                    for (Projection p : globalData.getProjections()) {
                        Element Projection = new Element("Projection");
                        // ajout un identifiant à la Projection
                        Projection.setAttribute("id", Long.toString(p.getId()));

                        // format date et heure de la Projection
                        String datePattern = "dd-MM-yyyy - HH:mm";
                        SimpleDateFormat dateFormatter = new SimpleDateFormat(datePattern);
                        cal.setTime(p.getDateHeure().getTime());
                        dateFormatter.format(cal.getTime());

                        String dateProj = "";
                        dateProj = dateFormatter.format(cal.getTime());
                        // élément datHeures
                        Element dateHeure = new Element("dateHeure").setText(
                                dateProj);

                        dateHeure.setAttribute(
                                "format", "dd-yy-yyyy - HH:mm");
                        // ajout du contenu dateHeure
                        Projection.addContent(dateHeure);
                        // Salle
                        Salle salle = p.getSalle();
                        Element elemSalle = new Element("salle");

                        elemSalle.setAttribute(
                                "id", Long.toString(salle.getId()));
                        elemSalle.addContent(
                                new Element("no").setText(salle.getNo()));
                        elemSalle.addContent(
                                new Element("taille").
                                        setText(Integer.toString(salle.getTaille())));
                        Projection.addContent(elemSalle);
                        // Film
                        Film film = p.getFilm();
                        Element elemFilm = new Element("film");

                        elemFilm.setAttribute(
                                "id", Long.toString(film.getId()));
                        elemFilm.addContent(
                                new Element("titre").setText(film.getTitre()));
                        elemFilm.addContent(
                                new Element("synopsis").setText(
                                        film.getSynopsis()));
                        elemFilm.addContent(
                                new Element("duree").setText(Integer.toString(film.getDuree())));
                        elemFilm.addContent(
                                new Element("photo").setText(film.getPhoto()));

                        // Critiques
                        Element elemCritiques = new Element("critiques");
                        for (Critique c
                                : film.getCritiques()) {
                            Element elemCritique = new Element("critique")
                                    .setAttribute("id", Long.toString(c.getId()));

                            elemCritique.setAttribute("note", Integer.toString(c.getNote()));
                            elemCritique.setText(c.getTexte());
                            elemCritiques.addContent(elemCritique);
                        }

                        elemFilm.addContent(elemCritiques);

                        // Mots clés
                        Element elemMotcles = new Element("motcles");
                        int limit = 5;
                        for (Motcle m
                                : film.getMotcles()) {
                            if (limit == 0)
                                break;

                            Element elemMotcle = new Element("motcle");
                            elemMotcle.setAttribute("id", Long.toString(m.getId()));
                            Element elemLabel = new Element("label").setText(m.getLabel());
                            elemMotcle.addContent(elemLabel);
                            elemMotcles.addContent(elemMotcle);
                            limit--;
                        }

                        elemFilm.addContent(elemMotcles);

                        // Genres
                        Element elemGenres = new Element("genres");
                        for (Genre g
                                : film.getGenres()) {

                            elemGenres.addContent(new Element("genre").addContent(new Element("label").setText(g.getLabel())));
                        }
                        elemFilm.addContent(elemGenres);

                        // Langues
                        Element elemLangages = new Element("langages");
                        for (Langage langue
                                : film.getLangages()) {
                            elemLangages.addContent(new Element("langage").addContent(new Element("label")).setText(
                                    langue.getLabel()));
                        }

                        elemFilm.addContent(elemLangages);


                        // Roles et role
                        Element elemRoles = new Element("Roles");
                        elemFilm.addContent(elemRoles);

                        for (RoleActeur r : film.getRoles()) {
                            if (r.getPlace() > 2)
                                continue;
                            Element elemRole = new Element("Role");
                            if (r.getPersonnage() != null)
                                elemRole.addContent(new Element("personnage")).setText(r.getPersonnage().toString());

                            elemRole.addContent(new Element("place")).setText(Long.toString(r.getPlace()));

                            elemRoles.addContent(elemRole);
                            Element elemActeur = new Element("Acteur");
                            elemActeur.addContent(new Element("nom").setText(r.getActeur().getNom()));
                            elemActeur.addContent(new Element("nomNaissance").setText(r.getActeur().getNomNaissance()));
                            elemActeur.addContent(new Element("biographie").setText(r.getActeur().getBiographie()));
                            if (r.getActeur().getDateNaissance() != null) {
                                cal.setTime(r.getActeur().getDateNaissance().getTime());
                            }
                            elemActeur.addContent(new Element("dateNaissance").setText(dfl.valueToString(cal)));
                            if (r.getActeur().getDateDeces() != null) {
                                cal.setTime(r.getActeur().getDateDeces().getTime());
                            }
                            elemActeur.addContent(new Element("dateDeces").setText(dfl.valueToString(cal)));

                            elemRole.addContent(elemActeur);
                        }


                        // Ajout du film à la Projection
                        Projection.addContent(elemFilm);

                        // Ajout de la Projection au Projections cinématographique
                        Projections.addContent(Projection);
                    }
                    try {
                        // Sortie fichier
                        XMLOutputter sortie = new XMLOutputter(Format.getPrettyFormat());
                        sortie.output(Projections, new FileOutputStream("SER_Lab2.xml"));
                        mainGUI.setAcknoledgeMessage("DONE in " + displaySeconds(currentTime, System.currentTimeMillis()));
                    } catch (Exception e) {
                        System.out.println("sortie fichier");
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    System.out.println("main");
                    e.printStackTrace();
                    mainGUI.setErrorMessage("Construction XML impossible", e.toString());
                }
            }
        }.start();
    }


    public void createXStreamXML() {
        new Thread() {
            public void run() {
                mainGUI.setAcknoledgeMessage("Creation XML... WAIT");
                long currentTime = System.currentTimeMillis();
                try {
                    globalData = ormAccess.GET_GLOBAL_DATA();
                    globalDataControle();
                } catch (Exception e) {
                    System.out.println("create xml");
                    e.printStackTrace();
                    mainGUI.setErrorMessage("Construction XML impossible", e.toString());
                }

                XStream xstream = new XStream();
                writeToFile("global_data.xml", xstream, globalData);
                System.out.println("Done [" + displaySeconds(currentTime, System.currentTimeMillis()) + "]");
                mainGUI.setAcknoledgeMessage("XML cree en " + displaySeconds(currentTime, System.currentTimeMillis()));
            }
        }.start();
    }

    private static void writeToFile(String filename, XStream serializer, Object data) {
        try {
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename), "UTF-8"));
            serializer.toXML(data, out);
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static final DecimalFormat doubleFormat = new DecimalFormat("#.#");

    private static final String displaySeconds(long start, long end) {
        long diff = Math.abs(end - start);
        double seconds = ((double) diff) / 1000.0;
        return doubleFormat.format(seconds) + " s";
    }

    private void globalDataControle() {
        for (Projection p : globalData.getProjections()) {
            System.out.println("******************************************");
            System.out.println(p.getFilm().getTitre());
            System.out.println(p.getSalle().getNo());
            System.out.println("Acteurs *********");
            for (RoleActeur role : p.getFilm().getRoles()) {
                System.out.println(role.getActeur().getNom());
            }
            System.out.println("Genres *********");
            for (Genre genre : p.getFilm().getGenres()) {
                System.out.println(genre.getLabel());
            }
            System.out.println("Mot-cles *********");
            for (Motcle motcle : p.getFilm().getMotcles()) {
                System.out.println(motcle.getLabel());
            }
            System.out.println("Langages *********");
            for (Langage langage : p.getFilm().getLangages()) {
                System.out.println(langage.getLabel());
            }
            System.out.println("Critiques *********");
            for (Critique critique : p.getFilm().getCritiques()) {
                System.out.println(critique.getNote());
                System.out.println(critique.getTexte());
            }
        }
    }
}



