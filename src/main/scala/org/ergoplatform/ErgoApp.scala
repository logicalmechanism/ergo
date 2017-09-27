package org.ergoplatform

import akka.actor.{ActorRef, Props}
import org.ergoplatform.modifiers.ErgoPersistentModifier
import org.ergoplatform.modifiers.mempool.AnyoneCanSpendTransaction
import org.ergoplatform.modifiers.mempool.proposition.AnyoneCanSpendProposition
import org.ergoplatform.nodeView.ErgoNodeViewHolder
import org.ergoplatform.nodeView.history.{ErgoSyncInfo, ErgoSyncInfoMessageSpec}
import org.ergoplatform.settings.ErgoSettings
import scorex.core.api.http.ApiRoute
import scorex.core.app.Application
import scorex.core.network.NodeViewSynchronizer
import scorex.core.network.message.MessageSpec
import scorex.core.settings.Settings

class ErgoApp(args: Seq[String]) extends Application {
  override type P = AnyoneCanSpendProposition.type
  override type TX = AnyoneCanSpendTransaction
  override type PMOD = ErgoPersistentModifier
  override type NVHT = ErgoNodeViewHolder[_]

  lazy val ergoSettings: ErgoSettings = ErgoSettings.read(args.headOption)

  //TODO remove after Scorex update
  override implicit lazy val settings: Settings = ergoSettings.scorexSettings

  override lazy val apiRoutes: Seq[ApiRoute] = Seq()
  override lazy val apiTypes: Set[Class[_]] = Set()

  override protected lazy val additionalMessageSpecs: Seq[MessageSpec[_]] = Seq()
  override val nodeViewHolderRef: ActorRef = ErgoNodeViewHolder.createActor(actorSystem, ergoSettings)

  override val localInterface: ActorRef = actorSystem.actorOf(Props(classOf[ErgoLocalInterface], nodeViewHolderRef))

  override val nodeViewSynchronizer: ActorRef = actorSystem.actorOf(
    Props(classOf[NodeViewSynchronizer[P, TX, ErgoSyncInfo, ErgoSyncInfoMessageSpec.type]],
    networkController, nodeViewHolderRef, localInterface, ErgoSyncInfoMessageSpec))
}

object ErgoApp extends App {
  new ErgoApp(args).run()
  def forceStopApplication(): Unit = new Thread(() => System.exit(1), "ergo-platform-shutdown-thread").start()
}
