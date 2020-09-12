package info.shibafu528.yukari.exvoice.runnable

import info.shibafu528.yukari.exvoice.MRuby
import info.shibafu528.yukari.exvoice.diva.ModelFactory
import info.shibafu528.yukari.exvoice.model.Message
import info.shibafu528.yukari.exvoice.model.User

import java.util.HashMap

fun twitter4j.Status.toMessage(mRuby: MRuby): Message {
    val values = HashMap<String, Any?>()
    values["id"] = this.id
    values["message"] = this.text
    values["user"] = this.user.toUser(mRuby)
    values["in_reply_to_user_id"] = this.inReplyToUserId
    values["in_reply_to_status_id"] = this.inReplyToStatusId
    values["retweet"] = this.retweetedStatus?.toMessage(mRuby)
    values["source"] = this.source
    values["created"] = this.createdAt
    values["modified"] = this.createdAt
    return ModelFactory.newInstance(mRuby, Message::class.java, "Message", values)
}

fun twitter4j.User.toUser(mRuby: MRuby): User {
    val values = HashMap<String, Any?>()
    values["id"] = this.id
    values["idname"] = this.screenName
    values["name"] = this.name
    values["location"] = this.location
    values["detail"] = this.description
    values["profile_image_url"] = this.profileImageURLHttps
    values["url"] = this.url
    values["protected"] = this.isProtected
    values["verified"] = this.isVerified
    values["followers_count"] = this.followersCount
    values["statuses_count"] = this.statusesCount
    values["friends_count"] = this.friendsCount
    return ModelFactory.newInstance(mRuby, User::class.java, "User", values)
}