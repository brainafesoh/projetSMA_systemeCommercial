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

import jade.core.AID;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

class InterfaceProducteur extends JFrame {	
	private AgentProducteur myAgent;
	
	private JTextField champTitre, champPrix;
	
	InterfaceProducteur(AgentProducteur a) {
		super(a.getLocalName());
		
		myAgent = a;
		
		JPanel p = new JPanel();
		p.setLayout(new GridLayout(2, 2));
		p.add(new JLabel("Nom du produit:"));
		champTitre = new JTextField(15);
		p.add(champTitre);
		p.add(new JLabel("prix:"));
		champPrix = new JTextField(15);
		p.add(champPrix);
		getContentPane().add(p, BorderLayout.CENTER);
		
		JButton addButton = new JButton("Add");
		addButton.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				try {
					String titre = champTitre.getText().trim();
					String prix = champPrix.getText().trim();
					myAgent.updateCatalogue(titre, Integer.parseInt(prix));
					champTitre.setText("");
					champPrix.setText("");
				}
				catch (Exception e) {
					JOptionPane.showMessageDialog(InterfaceProducteur.this, "Invalid values. "+e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE); 
				}
			}
		} );
		p = new JPanel();
		p.add(addButton);
		getContentPane().add(p, BorderLayout.SOUTH);
		
		// Deconnecter les agents lorsque l'utilisateur ferme les fenetres
		addWindowListener(new	WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				myAgent.doDelete();
			}
		} );
		
		setResizable(false);
	}
	
	public void afficheInterface() {
		pack();
		Dimension tailleEcran = Toolkit.getDefaultToolkit().getScreenSize();
		int centerX = (int)tailleEcran.getWidth() / 2;
		int centerY = (int)tailleEcran.getHeight() / 2;
		setLocation(centerX - getWidth() / 2, centerY - getHeight() / 2);
		super.setVisible(true);
	}	
}
