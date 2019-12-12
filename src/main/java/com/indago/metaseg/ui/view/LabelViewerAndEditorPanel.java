package com.indago.metaseg.ui.view;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.scijava.Context;
import org.scijava.app.StatusService;
import org.scijava.io.IOService;
import org.scijava.log.LogService;
import org.scijava.widget.WidgetService;

import com.indago.data.segmentation.LabelingSegment;
import com.indago.fg.Assignment;
import com.indago.metaseg.MetaSegContext;
import com.indago.metaseg.MetaSegLog;
import com.indago.metaseg.io.projectfolder.MetasegProjectFolder;
import com.indago.metaseg.ui.model.MetaSegCostPredictionTrainerModel;
import com.indago.metaseg.ui.model.MetaSegModel;
import com.indago.metaseg.ui.model.MetaSegSolverModel;
import com.indago.metaseg.ui.model.MetaSegTags;
import com.indago.pg.IndicatorNode;
import com.indago.pg.segments.SegmentNode;
import com.indago.plugins.seg.IndagoSegmentationPlugin;
import com.indago.plugins.seg.IndagoSegmentationPluginService;

import io.scif.codec.CodecService;
import io.scif.formats.qt.QTJavaService;
import io.scif.formats.tiff.TiffService;
import io.scif.img.IO;
import io.scif.img.ImgUtilityService;
import io.scif.img.converters.PlaneConverterService;
import io.scif.services.DatasetIOService;
import io.scif.services.FilePatternService;
import io.scif.services.FormatService;
import io.scif.services.InitializeService;
import io.scif.services.JAIIIOService;
import io.scif.services.LocationService;
import io.scif.services.TranslatorService;
import io.scif.xml.XMLService;
import net.imagej.DatasetService;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.ops.OpMatchingService;
import net.imagej.ops.OpService;
import net.imagej.patcher.LegacyInjector;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.IntArray;
import net.imglib2.roi.IterableRegion;
import net.imglib2.roi.Regions;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.util.ValuePair;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import net.miginfocom.swing.MigLayout;
import sc.fiji.labeleditor.core.model.LabelEditorModel;
import sc.fiji.labeleditor.plugin.behaviours.select.ConflictSelectionBehaviours;
import sc.fiji.labeleditor.plugin.mode.timeslice.TimeSliceLabelEditorBdvPanel;
import sc.fiji.labeleditor.plugin.mode.timeslice.TimeSliceLabelEditorModel;

public class LabelViewerAndEditorPanel< T extends RealType< T > > extends TimeSliceLabelEditorBdvPanel {

	private final MetaSegSolverModel model;
	private JButton btnContinueMetatrain;
	private JButton btnExportSegCompatibleImages;
	private JButton btnExportLabelFusionProblem;

	static {
		LegacyInjector.preinit();
	}

	public LabelViewerAndEditorPanel( MetaSegSolverModel solutionModel ) {
		this.model = solutionModel;
		Context context = solutionModel.getModel().getContext();
		context.inject( this );
		init( solutionModel.getModel().getRawData() );
	}

	public void populateBdv( MetaSegSolverModel solutionModel ) {
		LabelEditorModel labelEditorModel = buildLabelEditorModel( solutionModel );
		init( labelEditorModel );
		control().install( new ConflictSelectionBehaviours< T >() );
	}

	public static LabelEditorModel buildLabelEditorModel( MetaSegSolverModel model ) {
		ArrayImg< IntType, IntArray > backing =
				ArrayImgs.ints( model.getRawData().dimension( 0 ), model.getRawData().dimension( 1 ), model.getRawData().dimension( 2 ) );
		ImgLabeling< WrappedSegmentNode, IntType > labels = new ImgLabeling<>( backing );
		TimeSliceLabelEditorModel labelEditorModel = new TimeSliceLabelEditorModel<>(labels, model.getRawData(), 2);

		for ( int bdvTime = 0; bdvTime < model.getModel().getNumberOfFrames(); bdvTime++ ) {
			if ( model.getPgSolutions() != null && model.getPgSolutions().size() > bdvTime && model.getPgSolutions().get( bdvTime ) != null ) {
				IntervalView< LabelingType< WrappedSegmentNode > > slice = Views.hyperSlice( labels, 2, bdvTime );
				final Assignment< IndicatorNode > solution = model.getPgSolution( bdvTime );
				if ( solution != null ) {
					for ( final SegmentNode segVar : model.getProblems().get( bdvTime ).getSegments() ) {
						WrappedSegmentNode wrappedSegVar = new WrappedSegmentNode( segVar );
						IterableRegion< ? > region = segVar.getSegment().getRegion();
						Regions.sample( region, slice ).forEach( t -> t.add( wrappedSegVar ) );
						MetaSegTags tag;
						if ( solution.getAssignment( segVar ) == 1 ) {
							labelEditorModel.tagging().addTagToLabel( MetaSegTags.ILP_APPROVED, wrappedSegVar );
						} else {
							tag = MetaSegTags.ILP_DISAPPROVED;
						}

					}
				}

			}
			System.out.println(labelEditorModel.tagging().get().size());
		}

		labelEditorModel.colors().getFocusBorderColor().set( 255, 0, 0, 150);
		labelEditorModel.colors().getDefaultFaceColor().set(0,0,0,0);
		labelEditorModel.colors().getDefaultBorderColor().set(0,0,0,0);
		labelEditorModel.colors().getFaceColor( MetaSegTags.ILP_APPROVED ).set( 0, 255, 0, 150 );

		return labelEditorModel;
	}

///////////////////////////////////////////////////////////////////Stand alone demo of LabelEditor below /////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static void main( String... args ) {

		JPanel panel = buildMetaSegEditingPanel();
		JFrame frame = new JFrame( "Label editor" );
		JPanel parent = new JPanel(new MigLayout("fill"));
		frame.setContentPane( parent );
		frame.setMinimumSize( new Dimension( 500, 500 ) );
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		parent.add( panel, "push, grow, span" );
		frame.pack();
		frame.setVisible( true );
	}

	private static < T extends RealType< T > > JPanel buildMetaSegEditingPanel() {

		final Context context =
				new Context( FormatService.class, OpService.class, OpMatchingService.class, IOService.class, DatasetIOService.class, LocationService.class, DatasetService.class, ImgUtilityService.class, StatusService.class, TranslatorService.class, QTJavaService.class, TiffService.class, CodecService.class, JAIIIOService.class, LogService.class, IndagoSegmentationPluginService.class, PlaneConverterService.class, InitializeService.class, XMLService.class, FilePatternService.class, WidgetService.class );
		MetaSegContext.segPlugins = context.getService( IndagoSegmentationPluginService.class );

		Img input = IO.openImgs( LabelViewerAndEditorPanel.class.getResource( "/raw.tif" ).getPath() ).get( 0 );
		ImgPlus< T > data = new ImgPlus< T >( input, "input", new AxisType[] { Axes.X, Axes.Y, Axes.TIME } );
		MetasegProjectFolder projectFolder = null;
		try {
			projectFolder = new MetasegProjectFolder( new File( "/Users/prakash/Git-repos/metaseg/src/main/resources/data/metaseg_small" ) );
			projectFolder.initialize();
		} catch ( IOException e ) {
			e.printStackTrace();
		}

		final MetaSegModel model = new MetaSegModel( projectFolder, data );

		for ( final String name : MetaSegContext.segPlugins.getPluginNames() ) {
			final IndagoSegmentationPlugin segPlugin =
					MetaSegContext.segPlugins.createPlugin(
							name,
							model.getProjectFolder(),
							model.getRawData(),
							MetaSegLog.segmenterLog );
			if ( segPlugin.isUsable() ) {
				model.getSegmentationModel().addPlugin( segPlugin );
			}
		}

		//fetch segments:
		MetaSegCostPredictionTrainerModel costTrainerModel = model.getCostTrainerModel();
		costTrainerModel.createLabelingsFromScratch();
		costTrainerModel.getConflictGraphs();
		costTrainerModel.getConflictCliques();

		costTrainerModel.setAllSegAndCorrespTime();
		costTrainerModel.randomizeSegmentsAndPrepData();

		//mark a few good or bad
		List< ValuePair< LabelingSegment, Integer > > segms = costTrainerModel.getAllSegsWithTime();
		for ( int i = 0; i < 10; i++ ) {
			costTrainerModel.addToGood( segms.get( i ) );
		}
		for ( int i = 10; i < 18; i++ ) {
			costTrainerModel.addToBad( segms.get( i ) );
		}
		costTrainerModel.modifyPredictionSet();

		//compute solution

		try {
			costTrainerModel.startTrainingPhase();
		} catch ( Exception e ) {
			e.printStackTrace();
		}
		costTrainerModel.computeAllCosts();
		costTrainerModel.getParentModel().getSolutionModel().run();

		MetaSegLog.segmenterLog.info( "Done!" );


		MetaSegSolverModel solutionModel = model.getSolutionModel();
		LabelViewerAndEditorPanel< T > labelEditorPanel = new LabelViewerAndEditorPanel< T >( solutionModel );
		labelEditorPanel.populateBdv( solutionModel );
		return labelEditorPanel;
	}

////////////////////////////////////////////////////////////Demo of LabelEditor ends /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}