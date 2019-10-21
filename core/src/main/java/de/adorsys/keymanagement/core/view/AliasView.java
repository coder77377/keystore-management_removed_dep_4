package de.adorsys.keymanagement.core.view;

import com.googlecode.cqengine.IndexedCollection;
import com.googlecode.cqengine.TransactionalIndexedCollection;
import com.googlecode.cqengine.attribute.SimpleAttribute;
import com.googlecode.cqengine.attribute.SimpleNullableAttribute;
import com.googlecode.cqengine.index.Index;
import com.googlecode.cqengine.index.radix.RadixTreeIndex;
import com.googlecode.cqengine.query.Query;
import com.googlecode.cqengine.query.QueryFactory;
import com.googlecode.cqengine.query.parser.sql.SQLParser;
import de.adorsys.keymanagement.api.QueryResult;
import de.adorsys.keymanagement.api.source.KeySource;
import de.adorsys.keymanagement.api.types.ResultCollection;
import de.adorsys.keymanagement.api.types.entity.KeyAlias;
import de.adorsys.keymanagement.api.types.entity.metadata.KeyMetadata;
import lombok.Getter;
import lombok.SneakyThrows;

import java.util.Collection;
import java.util.Collections;

import static com.googlecode.cqengine.codegen.AttributeBytecodeGenerator.createAttributes;
import static com.googlecode.cqengine.codegen.MemberFilters.GETTER_METHODS_ONLY;
import static com.googlecode.cqengine.query.QueryFactory.attribute;
import static com.googlecode.cqengine.query.QueryFactory.nullableAttribute;
import static de.adorsys.keymanagement.core.view.ViewUtil.SNAKE_CASE;

public class AliasView extends UpdatingView<KeyAlias> {
    public static final SimpleAttribute<KeyAlias, String> A_ID = attribute("alias", KeyAlias::getAlias);
    public static final SimpleNullableAttribute<KeyAlias, KeyMetadata> META = nullableAttribute("meta", KeyAlias::getMeta);

    private static final SQLParser<KeyAlias> PARSER = SQLParser.forPojoWithAttributes(
            KeyAlias.class,
            createAttributes(KeyAlias.class, GETTER_METHODS_ONLY, SNAKE_CASE)
    );

    @Getter
    private final KeySource source;
    /**
     * Note that keystore aliases are case-insensitive in general case
     */
    private final IndexedCollection<KeyAlias> aliases = new TransactionalIndexedCollection<>(KeyAlias.class);

    public AliasView(KeySource source) {
        this(source, Collections.emptyList());
    }

    @SneakyThrows
    public AliasView(KeySource source, Collection<Index<KeyAlias>> indexes) {
        this.source = source;
        source.aliases().forEach(it -> aliases.add(new KeyAlias(it, null))); // FIXME Extract metadata
        this.aliases.addIndex(RadixTreeIndex.onAttribute(A_ID));
        indexes.forEach(aliases::addIndex);
    }

    @Override
    public QueryResult<KeyAlias> retrieve(Query<KeyAlias> query) {
        return new QueryResult<>(aliases.retrieve(query));
    }

    @Override
    public QueryResult<KeyAlias> retrieve(String query) {
        return new QueryResult<>(aliases.retrieve(PARSER.parse(query).getQuery()));
    }

    @Override
    public ResultCollection<KeyAlias> all() {
        return new QueryResult<>(aliases.retrieve(QueryFactory.all(KeyAlias.class))).toCollection();
    }

    @Override
    protected String getKeyId(KeyAlias ofKey) {
        return ofKey.getAlias();
    }

    @Override
    protected KeyAlias viewFromId(String ofKey) {
        return new KeyAlias(ofKey, null); // FIXME Extract metadata
    }

    @Override
    protected boolean updateCollection(Collection<KeyAlias> keysToRemove, Collection<KeyAlias> keysToAdd) {
        return aliases.update(Collections.emptyList(), keysToAdd);
    }
}
