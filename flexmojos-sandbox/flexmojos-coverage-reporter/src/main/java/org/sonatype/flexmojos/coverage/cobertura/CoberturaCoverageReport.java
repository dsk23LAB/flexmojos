package org.sonatype.flexmojos.coverage.cobertura;

import java.io.File;
import java.util.List;

import net.sourceforge.cobertura.coveragedata.ClassData;
import net.sourceforge.cobertura.coveragedata.CoverageDataFileHandler;
import net.sourceforge.cobertura.coveragedata.ProjectData;
import net.sourceforge.cobertura.reporting.ComplexityCalculator;
import net.sourceforge.cobertura.reporting.html.HTMLReport;
import net.sourceforge.cobertura.reporting.xml.SummaryXMLReport;
import net.sourceforge.cobertura.reporting.xml.XMLReport;
import net.sourceforge.cobertura.util.FileFinder;
import net.sourceforge.cobertura.util.Source;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.util.StringUtils;
import org.sonatype.flexmojos.coverage.AbstractCoverageReporter;
import org.sonatype.flexmojos.coverage.CoverageReportException;
import org.sonatype.flexmojos.coverage.CoverageReportRequest;
import org.sonatype.flexmojos.coverage.CoverageReporter;
import org.sonatype.flexmojos.coverage.util.ApparatUtil;
import org.sonatype.flexmojos.util.PathUtil;

import apparat.tools.coverage.CoverageObserver;

@Component( role = CoverageReporter.class, hint = "cobertura", instantiationStrategy = "per-lookup" )
public class CoberturaCoverageReport
    extends AbstractCoverageReporter
    implements CoverageReporter, Initializable
{

    private ProjectData coverageProjectData;

    public void initialize()
        throws InitializationException
    {
        this.coverageProjectData = new ProjectData();
    }

    @Override
    protected CoverageObserver getInstumentationObserver()
    {
        return new CoverageObserver()
        {
            public void instrument( String file, int line )
            {
                ClassData classData = coverageProjectData.getOrCreateClassData( ApparatUtil.toClassname( file ) );
                classData.addLine( line, null, null );
            }
        };
    }

    public void generateReport( CoverageReportRequest request )
        throws CoverageReportException
    {
        File dataDirectory = request.getDataDirectory();

        FileFinder finder = new FileFinder()
        {
            public Source getSource( String fileName )
            {
                Source source = super.getSource( fileName.replace( ".java", ".as" ) );
                if ( source == null )
                {
                    source = super.getSource( fileName.replace( ".java", ".mxml" ) );
                }
                return source;
            }
        };

        List<File> sp = request.getSourcePaths();
        for ( File dir : sp )
        {
            finder.addSourceDirectory( PathUtil.path( dir ) );
        }

        ComplexityCalculator complexity = new ComplexityCalculator( finder );
        try
        {
            File coverageReportDestinationDir = request.getReportDestinationDir();
            coverageReportDestinationDir.mkdirs();

            List<String> format = request.getFormats();
            if ( format.contains( "html" ) )
            {
                String coverageReportEncoding = request.getReportEncoding();
                if ( StringUtils.isEmpty( coverageReportEncoding ) )
                {
                    coverageReportEncoding = "UTF-8";
                }
                new HTMLReport( coverageProjectData, coverageReportDestinationDir, finder, complexity,
                                coverageReportEncoding );
            }
            else if ( format.contains( "xml" ) )
            {
                new XMLReport( coverageProjectData, coverageReportDestinationDir, finder, complexity );
            }
            else if ( format.contains( "summaryXml" ) )
            {
                new SummaryXMLReport( coverageProjectData, coverageReportDestinationDir, finder, complexity );
            }
        }
        catch ( Exception e )
        {
            throw new CoverageReportException( "Unable to write coverage report", e );
        }

        CoverageDataFileHandler.saveCoverageData( coverageProjectData, new File( dataDirectory, "cobertura.ser" ) );
    }

    public void addResult( String classname, Integer[] touchs )
    {
        ClassData classData = this.coverageProjectData.getOrCreateClassData( ApparatUtil.toClassname( classname ) );
        for ( Integer touch : touchs )
        {
            classData.touch( touch, 1 );
        }
    }

}
