package models

import sangria.schema._

object SchemaDefinition {

    val IMAGE_URLS = Argument("urls", ListInputType(StringType), description = "image urls")

    val Query = ObjectType("Query", fields[ImageUrls, Unit](
        Field(
            "jobId",
            OptionType(StringType),
            arguments = IMAGE_URLS :: Nil,
            resolve = ctx => {
                ctx.ctx.processUrls(ctx.arg("urls"))
            }
        )
    ))

    val UrlSchema = Schema(Query)
}
