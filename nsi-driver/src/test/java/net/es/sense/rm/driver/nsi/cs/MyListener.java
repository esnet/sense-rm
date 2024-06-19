package net.es.sense.rm.driver.nsi.cs;

import lombok.extern.slf4j.Slf4j;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelChangedListener;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

import java.util.List;

@Slf4j
class MyListener implements ModelChangedListener
{
  @Override
  public void addedStatement( Statement s )
  {
    log.debug(">> added statement {}", s);
  }

  @Override
  public void addedStatements( Statement [] statements ) {
    log.debug(">> added statements:");
    for (Statement statement : statements) {
      log.debug(">>     {}", statement);
    }
  }

  @Override
  public void addedStatements( List statements ) {
    log.debug(">> added statements:");
    while (statements.iterator().hasNext()) {
      Object next = statements.iterator().next();
      log.debug(">>     {}", next.getClass().getName());
    }
  }

  @Override
  public void addedStatements( StmtIterator statements ) {
    log.debug(">> added statements:");
    statements.forEach(s -> {
      log.debug(">>     {}", s);
    });
  }
  @Override
  public void addedStatements( Model m ) {
    log.debug(">> added statements model");
  }
  @Override
  public void removedStatement( Statement s ) {
    log.debug(">> removed statement {}", s);
  }
  @Override
  public void removedStatements( Statement [] statements ) {
    log.debug(">> removed statements:");
    for (Statement statement : statements) {
      log.debug(">>     {}", statement);
    }
  }
  @Override
  public void removedStatements( List statements ) {
    log.debug(">> removed statements:");
    while (statements.iterator().hasNext()) {
      Object next = statements.iterator().next();
      log.debug(">>     {}", next.getClass().getName());
    }
  }
  @Override
  public void removedStatements( StmtIterator statements ) {
    log.debug(">> removed statements:");
    statements.forEach(s -> {
      log.debug(">>     {}", s);
    });
  }
  @Override
  public void removedStatements( Model m ) {
    log.debug(">> removed statements model");
  }

  @Override
  public void notifyEvent(Model model, Object o) {
    log.debug(">> notifyEvent: {}", o.getClass().getName());
  }
}