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
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

public class AgentAcheteur extends Agent {
	// Nom du produit a commander
	private String produitCible;
	// Liste d'agents producteurs connues
	private AID[] agentsProd;

	// Put agent initializations here
	protected void setup() {
		// Message d'acceuil
		System.out.println("Bienvenue agent acheteur: "+getAID().getName());

		// On recuper le nom du produit cible comme argument au debut
		Object[] args = getArguments();
		if (args != null && args.length > 0) {
			produitCible = (String) args[0];
			System.out.println("Le produit cible c'est "+produitCible);

			// Ajout d'un TickerBehaviour: fonction qui automatise une requete vers les agents de production toutes les minutes
			addBehaviour(new TickerBehaviour(this, 60000) {
				protected void onTick() {
					System.out.println("Essai d'achat du produit: "+produitCible);
					// Mise a jour des a agents de prodution
					DFAgentDescription template = new DFAgentDescription();
					ServiceDescription sd = new ServiceDescription();
					sd.setType("venteProduit");
					template.addServices(sd);
					try {
						DFAgentDescription[] resultat = DFService.search(myAgent, template); 
						System.out.println("Agents de production disponibles:");
						agentsProd = new AID[resultat.length];
						for (int i = 0; i < resultat.length; ++i) {
							agentsProd[i] = resultat[i].getName();
							System.out.println(agentsProd[i].getName());
						}
					}
					catch (FIPAException fe) {
						fe.printStackTrace();
					}

					// Effectuer la requete
					myAgent.addBehaviour(new ImplementeRequete());
				}
			} );
		}
		else {
			// On termine le processus dans le cas echeant
			System.out.println("Nom du produit cible non specifié");
			doDelete();
		}
	}

	// Opérations de nettoyage de l'agent
	protected void arret() {
		// Message de fin
		System.out.println("Arret agent acheteur "+getAID().getName());
	}

	/**
	   Classe interne ImplementeRequete.
	   C'est l'implementation des requetes utilisées par les agents acheteurs pour
	   rechercher la disponbilite d'un produit
	 */
	private class ImplementeRequete extends Behaviour {
		private AID meilleureOffre; // L'agent qui fait la meilleure offre 
		private int meilleurPrix;  // Meilleur prix proposé
		private int compteurReponses = 0; // Le compteur des réponses des agents de production
		private MessageTemplate mt; // Template recevant les reponses
		private int etape = 0;

		public void action() {
			switch (etape) {
			case 0:
				// Envoi des requetes a tout agents producteur
				ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
				for (int i = 0; i < agentsProd.length; ++i) {
					cfp.addReceiver(agentsProd[i]);
				} 
				cfp.setContent(produitCible);
				cfp.setConversationId("offreProduit");
				cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Identifiant unique
				myAgent.send(cfp);
				// Initialisation du template pour la reception des offres
				mt = MessageTemplate.and(MessageTemplate.MatchConversationId("offreProduit"),
						MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
				etape = 1;
				break;
			case 1:
				// Recevoir toutes les offres/rejets des agents de production
				ACLMessage reply = myAgent.receive(mt);
				if (reply != null) {
					// reponse recu
					if (reply.getPerformative() == ACLMessage.PROPOSE) {
						// Voici une offre 
						int prix = Integer.parseInt(reply.getContent());
						if (meilleureOffre == null || prix < meilleurPrix) {
							// Meilleure offre pour le moment
							meilleurPrix = prix;
							meilleureOffre = reply.getSender();
						}
					}
					compteurReponses++;
					if (compteurReponses >= agentsProd.length) {
						// Tout nos agents de production ont repondus
						etape = 2; 
					}
				}
				else {
					block();
				}
				break;
			case 2:
				// Emmettre le bon d'achat a l'agent de production ayant fourni la meilleure offre
				ACLMessage commande = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
				commande.addReceiver(meilleureOffre);
				commande.setContent(produitCible);
				commande.setConversationId("offreProduit");
				commande.setReplyWith("commande"+System.currentTimeMillis());
				myAgent.send(commande);
				// Initialiser le template pour la reponse au bon d'achat emmit
				mt = MessageTemplate.and(MessageTemplate.MatchConversationId("offreProduit"),
						MessageTemplate.MatchInReplyTo(commande.getReplyWith()));
				etape = 3;
				break;
			case 3:      
				// Reception de la reponse au bon d'achat
				reply = myAgent.receive(mt);
				if (reply != null) {
					// Reponse recu
					if (reply.getPerformative() == ACLMessage.INFORM) {
						// Commande effectué. Nous pouvons a present terminer
						System.out.println(produitCible+" commandé avec succes au pres de l'agent "+reply.getSender().getName());
						System.out.println("prix = "+meilleurPrix);
						myAgent.doDelete();
					}
					else {
						System.out.println("Echec de Commande: le stock du produit demandé est écoulé.");
					}

					etape = 4;
				}
				else {
					block();
				}
				break;
			}        
		}

		public boolean done() {
			if (etape == 2 && meilleureOffre == null) {
				System.out.println("Echec: "+produitCible+" indisponible");
			}
			return ((etape == 2 && meilleureOffre == null) || etape == 4);
		}
	}  // Fin classe interne ImplementeRequete
}
