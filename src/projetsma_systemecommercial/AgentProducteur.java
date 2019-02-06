/*****************************************************************
JADE - Java Agent DEvelopment Framework is a framework to develop 
multi-agent systems in compliance with the FIPA specifications.
Copyright (C) 2000 CSELT S.p.A. 

GNU Lesser General Public License

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation, 
version 2.1 of the License. 

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the
Free Software Foundation, Inc., 59 Temple Place - Suite 330,
Boston, MA  02111-1307, USA.
 *****************************************************************/

package projetsma_systemecommercial;

import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import java.util.*;

public class AgentProducteur extends Agent {
	// Catalogue de produits precisant pour chacun, la qte disponible et le prix
	private Hashtable catalogue;
	// interface graphique par lequel on ajoutera des produits dans le catalogue
	private InterfaceProducteur intProd;

	// On initialise les agents
	protected void setup() {
		// Creation du catalogue
		catalogue = new Hashtable();

		// Creation et affichage de l'interface graphique
		intProd = new InterfaceProducteur(this);
		intProd.afficheInterface();

		// On enregistre le service venteProduit
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("venteProduit");
		sd.setName("commerce");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}

		// On ajoute le comportement des requetes periodiques des agents acheteurs.
		addBehaviour(new ServeurDemandeOffres());

		// Ajout du comportement des requetes des commandes des agents acheteurs
		addBehaviour(new ServeurCommnadesAchat());
	}

	// opérations de nettoyage de l'agent
	protected void arret() {
		// Desinscription
		try {
			DFService.deregister(this);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
		// fermeture de l'interface graphique
		intProd.dispose();
		// afficher un message d'aurevoir
		System.out.println("Agent producteur "+getAID().getName()+" deconnexion.");
	}

	/**
	 Cette methode est invoquée par l'interface graphique
	 lorsqu'un produit ajoute un nouveau produit a vendre.
	 */
	public void updateCatalogue(final String titre, final int prix) {
		addBehaviour(new OneShotBehaviour() {
			public void action() {
				catalogue.put(titre, new Integer(prix));
				System.out.println(titre+" inserted into catalogue. prix = "+prix);
			}
		} );
	}

	/**
	   Inner class .
	   Classe interne ServeurDemandeOffres
	   C'est dans cette classe regit le comportement permettant aux agents producteurs
	   de repondre aux requetes pour offres emmis par les agents acheteurs.
	   Si le produit demandé est dans le catalogue de l'agent producteur, il repond avec
	   un message de PROPOSITION en precisant le prix. Sinon, il repond avec un message de REFUS
	 */
	private class ServeurDemandeOffres extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				// Traitement du message recu
				String titre = msg.getContent();
				ACLMessage reply = msg.createReply();

				Integer prix = (Integer) catalogue.get(titre);
				if (prix != null) {
					// Le produit demandé est disponible. On repond avec son prix.
					reply.setPerformative(ACLMessage.PROPOSE);
					reply.setContent(String.valueOf(prix.intValue()));
				}
				else {
					// Produit indisponible
					reply.setPerformative(ACLMessage.REFUSE);
					reply.setContent("Produit indisponible");
				}
				myAgent.send(reply);
			}
			else {
				block();
			}
		}
	}  // Fin classe interne ServeurDemandeOffres

	/**
	   Classe interne ServeurCommnadesAchat.
	   Ce comportement est utilisé par les agents Producteurs pour servir ou
	   prendre en charge les commandes des agents Acheteurs apres avoir accepté  l'offre.
	   De ce fait, l'agent Producteur reduit la qte disponible du produit dans son catalogue
	   puis repond avec un message INFORMATIF le notifiant que la transaction a été effectuée.
	 */
	private class ServeurCommnadesAchat extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				// traitement du message recu. 
				String titre = msg.getContent();
				ACLMessage reply = msg.createReply();

				Integer prix = (Integer) catalogue.remove(titre);
				if (prix != null) {
					reply.setPerformative(ACLMessage.INFORM);
					System.out.println(titre+" vendu a l'agent "+msg.getSender().getName());
				}
				else {
					// le produit demandé a été vendu a un autre acheteur
					reply.setPerformative(ACLMessage.FAILURE);
					reply.setContent("produit non-disponible");
				}
				myAgent.send(reply);
			}
			else {
				block();
			}
		}
	}  // fin classe interne ServeurDemandeOffres
}
