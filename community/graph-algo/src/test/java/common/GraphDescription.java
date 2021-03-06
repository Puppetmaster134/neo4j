/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;

public class GraphDescription implements GraphDefinition
{

    private static class RelationshipDescription
    {
        private final String end;
        private final String start;
        private final RelationshipType type;

        RelationshipDescription( String rel )
        {
            String[] parts = rel.split( " " );
            if ( parts.length != 3 )
            {
                throw new IllegalArgumentException( "syntax error: \"" + rel
                                                    + "\"" );
            }
            start = parts[0];
            type = RelationshipType.withName( parts[1] );
            end = parts[2];
        }

        public Relationship create( Transaction transaction, Map<String, Node> nodes )
        {
            Node startNode = getNode( transaction, nodes, start );
            Node endNode = getNode( transaction, nodes, end );
            return startNode.createRelationshipTo( endNode, type );
        }

        private Node getNode( Transaction transaction, Map<String, Node> nodes, String name )
        {
            Node node = nodes.get( name );
            if ( node == null )
            {
                if ( nodes.size() == 0 )
                {
                    node = transaction.createNode();
                }
                else
                {
                    node = transaction.createNode();
                }
                node.setProperty( "name", name );
                nodes.put( name, node );
            }
            return node;
        }
    }

    private final RelationshipDescription[] description;

    public GraphDescription( String... description )
    {
        List<RelationshipDescription> lines = new ArrayList<>();
        for ( String part : description )
        {
            for ( String line : part.split( "\n" ) )
            {
                lines.add( new RelationshipDescription( line ) );
            }
        }
        this.description = lines.toArray( new RelationshipDescription[0] );
    }

    @Override
    public Node create( GraphDatabaseService graphdb )
    {
        Map<String, Node> nodes = new HashMap<>();
        Node node = null;
        try ( Transaction tx = graphdb.beginTx() )
        {
            for ( RelationshipDescription rel : description )
            {
                node = rel.create( tx, nodes ).getEndNode();
            }
            tx.commit();
        }
        return node;
    }
}
