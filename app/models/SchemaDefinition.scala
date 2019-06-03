package models

import sangria.schema._

object SchemaDefinition {

    val ImageUrls = Argument("urls", ListInputType(StringType), description = "image urls")

    val Query = ObjectType("Query", fields[ImageUrls, Unit](
        Field(
            "jobId",
            OptionType(StringType),
            arguments = ImageUrls :: Nil,
            resolve = ctx => {
                ctx.ctx.processUrls(ctx.arg("urls"))
            }
        )
    ))

    val UrlSchema = Schema(Query)
}
