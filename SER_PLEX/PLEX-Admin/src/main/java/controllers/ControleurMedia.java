package controllers;

import com.google.gson.*;
import models.*;
import views.*;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Calendar;

public class ControleurMedia {

	private ControleurGeneral ctrGeneral;
	private static MainGUI mainGUI;
	private ORMAccess ormAccess;
	
	private GlobalData globalData;

	public ControleurMedia(ControleurGeneral ctrGeneral, MainGUI mainGUI, ORMAccess ormAccess){
		this.ctrGeneral=ctrGeneral;
		ControleurMedia.mainGUI=mainGUI;
		this.ormAccess=ormAccess;
	}

	public void sendJSONToMedia(){
		new Thread(){
			public void run(){
				mainGUI.setAcknoledgeMessage("Envoi JSON ... WAIT");
				//long currentTime = System.currentTimeMillis();
				try {
					globalData = ormAccess.GET_GLOBAL_DATA();

					// Crée un objet Json qui contiendra toutes les projections
					JsonObject projections = new JsonObject();

					// Tableau des projections
					JsonArray projectionsArray = new JsonArray();

					// globalData contient toutes les projections. On les parcourt une à une et récupérons les informations voulues
					for (Projection p : globalData.getProjections()) {
						// Déclaration d'une projection
						JsonObject projection = new JsonObject();

						// On récupère la date et formatage
						Calendar cal = Calendar.getInstance();
						cal.setTime(p.getDateHeure().getTime());
						DateLabelFormatter dlf = new DateLabelFormatter();
						String date = dlf.valueToString(cal);

						// On récupère le titre
						String titre = p.getFilm().getTitre().toString();

						// Une projection ayant plusieurs acteurs, nous travaillons avec un JsonArray
						JsonArray acteurs = new JsonArray();

						JsonObject film = new JsonObject();

						// On parcourt tous les acteurs pour les ajouter à notre JsonArray
						for (RoleActeur r : p.getFilm().getRoles()) {

							// Ne récupère que les 10 acteurs les plus significatifs
							if (r.getPlace() > 10) {
								continue;
							}

							// On récupère le nom de l'acteur sous forme de String
							String act = r.getActeur().getNom();

							// On en crée sa primitive
							JsonPrimitive jsp = new JsonPrimitive(act);

							// On l'ajoute au tableau d'acteurs
							acteurs.add(jsp);
						}

						//On récupère l'id de l'acteur sous forme d'un entier
						long id = p.getId();

						// On ajoute les informations récupérées à notre Projection
						projection.addProperty("id", Long.toString(id));
						projection.addProperty("dateHeure", date);

						film.addProperty("titre", titre);
						film.add("acteurs", acteurs);

						projection.add("film", film);

						// On ajoute la projection au tableau de projections
						projectionsArray.add(projection);
					}

					// Le tableau de projections est maintenant complet, on l'ajoute à notre JsonObject projections
					projections.add("Projections", projectionsArray);

					//System.out.println("Projection ugly format : \n" + projections);
					// On convertit en "pretty format" notre objet qui est stocké dans un String
					// disableHtmlEscaping permet de fixer l'encodage utilisé par l'objet gson en UTF-8 afin de correctement afficher les caractères spéciaux
					Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
					String projectionsPrettyFormat = gson.toJson(projections);

					//System.out.println("Projection pretty format : \n" + projectionsPrettyFormat);
					// Nous créons notre fichier .json. Pour des questions d'optimisation, nous n'effectuons qu'une seule écriture
					//  de la string contenant l'ensemble du fichier. L'encodage est fixé en UTF-8
					BufferedWriter buf = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("SER_Lab2.json"), "UTF-8"));

					// Ecrit notre projection correctement formatée dans le fichier
					buf.write(projectionsPrettyFormat);

					// Fermeture et nettoyage du buffer
					buf.flush();
					buf.close();

					mainGUI.setAcknoledgeMessage("Envoi JSON: Terminé !");

				}
				catch (Exception e){
					mainGUI.setErrorMessage("Construction XML impossible", e.toString());
				}
			}
		}.start();
	}

}