/**
 *
 */
package com.indago.metaseg.ui.view;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTextField;

import com.indago.metaseg.MetaSegLog;
import com.indago.metaseg.ui.model.MetaSegCostPredictionTrainerModel;

import bdv.util.Bdv;
import bdv.util.BdvHandlePanel;
import net.miginfocom.swing.MigLayout;

/**
 * @author jug
 */
public class MetaSegCostPredictionTrainerPanel extends JPanel implements ActionListener, FocusListener {

	private static final long serialVersionUID = 3940247743127023839L;

	MetaSegCostPredictionTrainerModel model;

	private JSplitPane splitPane;
	private JButton btnFetch;
	private JButton btnPrepareTrainData;

	private JTextField txtMaxPixelComponentSize;

	private JTextField txtMinPixelComponentSize;

	private JButton btnComputeSoln;

	private ButtonGroup trainingModeButtons;

	private JSlider transparencySlider;

	private JCheckBox boxContinuousRetrain;

	public MetaSegCostPredictionTrainerPanel( final MetaSegCostPredictionTrainerModel costTrainerModel ) {
		super( new BorderLayout() );
		this.model = costTrainerModel;
		buildGui();
	}

	private void buildGui() {
		final JPanel viewer = new JPanel( new BorderLayout() );

		model.bdvSetHandlePanel(
				new BdvHandlePanel( ( Frame ) this.getTopLevelAncestor(), Bdv
						.options()
						.is2D() ) );

		viewer.add( model.bdvGetHandlePanel().getViewerPanel(), BorderLayout.CENTER );
		model.populateBdv();

		final MigLayout layout = new MigLayout( "", "[][grow]", "" );
		final JPanel controls = new JPanel( layout );

		final JPanel panelFetch = new JPanel( new MigLayout() );
		panelFetch.setBorder( BorderFactory.createTitledBorder( "segmentation fetching" ) );

		txtMaxPixelComponentSize = new JTextField( 5 );
		txtMaxPixelComponentSize.setText( Integer.toString( model.getMaxPixelComponentSize() ) ); //TODO Needs changing later to image size -1
		txtMaxPixelComponentSize.addActionListener( this );
		txtMaxPixelComponentSize.addFocusListener( this );
		txtMinPixelComponentSize = new JTextField( 5 );
		txtMinPixelComponentSize.setText( Integer.toString( model.getMinPixelComponentSize() ) );
		txtMinPixelComponentSize.addActionListener( this );
		txtMinPixelComponentSize.addFocusListener( this );

		btnFetch = new JButton( "fetch segments" );
		btnFetch.addActionListener( this );

		panelFetch.add( new JLabel( "Max segment size:" ), "growx" );
		panelFetch.add( txtMaxPixelComponentSize, "growx, wrap" );
		panelFetch.add( new JLabel( "Min segment size:" ), "growx" );
		panelFetch.add( txtMinPixelComponentSize, "growx, wrap" );
		panelFetch.add( btnFetch, "growx, wrap" );

		final JPanel panelPrepareTrainData = new JPanel( new MigLayout() );
		panelPrepareTrainData.setBorder( BorderFactory.createTitledBorder( "data prep" ) );

		btnPrepareTrainData = new JButton( "prepare training data" );
		btnPrepareTrainData.addActionListener( this );
		panelPrepareTrainData.add( btnPrepareTrainData, "growx, wrap" );

		final JPanel panelTrain = new JPanel( new MigLayout() );
		panelTrain.setBorder( BorderFactory.createTitledBorder( "training" ) );

		trainingModeButtons = new ButtonGroup();
		JRadioButton bRandom = new JRadioButton( "random" );
		JRadioButton bActiveLearningNormal = new JRadioButton( "active learning (normal)" );
		JRadioButton bActiveLeraningWithBalance = new JRadioButton( "active learning (class balance)" );
		bRandom.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent e ) {
				model.setALMode( "random" );
			}
		} );
		bActiveLearningNormal.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent e ) {
				model.setALMode( "active learning (normal)" );

			}
		} );
		bActiveLeraningWithBalance.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent e ) {
				model.setALMode( "active learning (class balance)" );
			}
		} );

		trainingModeButtons.add( bRandom );
		trainingModeButtons.add( bActiveLearningNormal );
		trainingModeButtons.add( bActiveLeraningWithBalance );

		boxContinuousRetrain = new JCheckBox( "continuous retrain" );

		panelTrain.add( bRandom, "span 2, growx, wrap" );
		panelTrain.add( bActiveLearningNormal, "span 2, growx, wrap" );
		panelTrain.add( bActiveLeraningWithBalance, "span 2, gapbottom 15, growx, wrap" );
		panelTrain.add( boxContinuousRetrain, "growx, wrap" );

		final JPanel panelCostPrediction = new JPanel( new MigLayout() );
		panelCostPrediction.setBorder( BorderFactory.createTitledBorder( "compute" ) );

		btnComputeSoln = new JButton( "compute solution" );
		btnComputeSoln.addActionListener( this );

		panelCostPrediction.add( btnComputeSoln, "growx, wrap" );

		controls.add( panelFetch, "growx, wrap" );
		controls.add( panelPrepareTrainData, "growx, wrap" );
		controls.add( panelTrain, "growx, wrap" );
		controls.add( panelCostPrediction, "growx, wrap" );

		bActiveLeraningWithBalance.doClick();

		final JSplitPane splitPane = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, controls, viewer );
		splitPane.setResizeWeight( 0.1 ); // 1.0 == extra space given to left component alone!
		this.add( splitPane, BorderLayout.CENTER );
	}


	/**
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed( final ActionEvent e ) {
		if (e.getSource().equals( btnFetch )) {
			actionFetch();
		} else if ( e.getSource().equals( btnPrepareTrainData ) ) {
			actionFetchForManualClassify();
		} else if ( e.getSource().equals( btnComputeSoln ) ) {
			try {
				actionComputeAllCostsAndRunSolver();
			} catch ( Exception e1 ) {
				e1.printStackTrace();
			}

		}
	}

	private void actionComputeAllCostsAndRunSolver() throws Exception {
		MetaSegLog.log.info( "Starting MetaSeg optimization..." );
		model.bdvRemoveAll();
		model.bdvAdd( model.getParentModel().getRawData(), "RAW" );
		model.startTrainingPhase();
		model.computeAllCosts();
		model.getParentModel().getSolutionModel().run();
		model.getParentModel().getMainPanel().getTabs().setSelectedComponent( model.getParentModel().getMainPanel().getTabSolution() );
		MetaSegLog.segmenterLog.info( "Done!" );
		model.getParentModel().getSolutionModel().populateBdv();
	}

	private void actionFetchForManualClassify() {
		MetaSegLog.log.info( "Fetching random segments for manual classification..." );
		model.randomizeSegmentHypotheses();
		model.getTrainingData();
	}

	private void actionFetch() {

//		parseAndSetParametersInModel();
		model.getLabelings();
		model.getConflictGraphs();
		model.getConflictCliques();
		MetaSegLog.log.info( "Segmentation results fetched!" );
	}


	@Override
	public void focusGained( FocusEvent e ) {}

	@Override
	public void focusLost( FocusEvent e ) {
		if ( e.getSource().equals( txtMaxPixelComponentSize ) || e.getSource().equals( txtMinPixelComponentSize ) ) {
			parseAndSetParametersInModel();
//				model.saveStateToFile();
		}

	}

	private void parseAndSetParametersInModel() {
		try {
			if ( txtMaxPixelComponentSize.getText().trim().isEmpty() ) {
				model.setMaxPixelComponentSize( Integer.MAX_VALUE );
			} else {
				model.setMaxPixelComponentSize( Integer.parseInt( txtMaxPixelComponentSize.getText() ) );
			}
		} catch ( final NumberFormatException e ) {
			txtMaxPixelComponentSize.setText( "" + model.getMaxPixelComponentSize() );
		}
		try {
			if ( txtMinPixelComponentSize.getText().trim().isEmpty() ) {
				model.setMinPixelComponentSize( model.getMinPixelComponentSize() );
			} else {
				model.setMinPixelComponentSize( Integer.parseInt( txtMinPixelComponentSize.getText() ) );
			}
		} catch ( final NumberFormatException e ) {
			txtMinPixelComponentSize.setText( "" + model.getMinPixelComponentSize() );
		}

	}

}
